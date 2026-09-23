package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.report.ConsumptionSummaryResponseDto;
import com.asalkartechworks.daddysodahouse.dto.report.FlavourConsumptionDto;
import com.asalkartechworks.daddysodahouse.dto.report.MachineConsumptionDto;
import com.asalkartechworks.daddysodahouse.dto.report.ManualFlavourSalesSummaryDto;
import com.asalkartechworks.daddysodahouse.entity.IotEventItem;
import com.asalkartechworks.daddysodahouse.entity.ManualDailyFlavourSales;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.exception.AuthException;
import com.asalkartechworks.daddysodahouse.repository.IotEventItemRepository;
import com.asalkartechworks.daddysodahouse.repository.IotEventRepository;
import com.asalkartechworks.daddysodahouse.repository.ManualDailyFlavourSalesRepository;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.util.CsvExportUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service managing consumption summary aggregation and CSV export across IoT and manual sales sources.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumptionReportService {

    private final IotEventRepository iotEventRepository;
    private final IotEventItemRepository iotEventItemRepository;
    private final ManualDailyFlavourSalesRepository manualDailySalesRepository;

    /**
     * Aggregates consumption metrics across IoT telemetry and manual sales entries.
     */
    @Transactional(readOnly = true)
    public ConsumptionSummaryResponseDto getConsumptionSummary(
            UUID requestedFranchiseId,
            UUID machineId,
            UUID productId,
            LocalDate fromDate,
            LocalDate toDate,
            String source,
            UserPrincipal principal
    ) {
        UUID effectiveFranchiseId = resolveEffectiveFranchiseId(requestedFranchiseId, principal);
        String normalizedSource = normalizeSource(source);

        Instant fromDateTime = fromDate != null ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant() : null;

        BigDecimal totalSyrupUsageMl = BigDecimal.ZERO;
        BigDecimal totalRawSodaUsageMl = BigDecimal.ZERO;
        List<FlavourConsumptionDto> flavourWiseIotUsage = new ArrayList<>();
        List<MachineConsumptionDto> machineWiseIotUsage = new ArrayList<>();

        if ("IOT".equals(normalizedSource) || "ALL".equals(normalizedSource)) {
            totalSyrupUsageMl = iotEventRepository.sumTotalSyrupMl(effectiveFranchiseId, machineId, fromDateTime, toDateTime);
            totalRawSodaUsageMl = iotEventRepository.sumTotalRawSodaMl(effectiveFranchiseId, machineId, fromDateTime, toDateTime);

            List<Object[]> flavourUsageRows = iotEventItemRepository.findFlavourSyrupUsage(
                    effectiveFranchiseId, machineId, productId, fromDateTime, toDateTime);

            for (Object[] row : flavourUsageRows) {
                UUID pId = (UUID) row[0];
                String itemCode = (String) row[1];
                String flavourName = (String) row[2];
                BigDecimal usageMl = (BigDecimal) row[3];

                BigDecimal percentage = BigDecimal.ZERO;
                if (totalSyrupUsageMl != null && totalSyrupUsageMl.compareTo(BigDecimal.ZERO) > 0) {
                    percentage = usageMl.multiply(BigDecimal.valueOf(100))
                            .divide(totalSyrupUsageMl, 2, RoundingMode.HALF_UP);
                }

                flavourWiseIotUsage.add(FlavourConsumptionDto.builder()
                        .productId(pId)
                        .itemCode(itemCode)
                        .flavourName(flavourName)
                        .syrupUsageMl(usageMl)
                        .usagePercentage(percentage)
                        .build());
            }

            List<Object[]> machineUsageRows = iotEventItemRepository.findMachineUsage(
                    effectiveFranchiseId, machineId, productId, fromDateTime, toDateTime);

            for (Object[] row : machineUsageRows) {
                String mCode = (String) row[0];
                String mName = (String) row[1];
                BigDecimal syrupMl = (BigDecimal) row[2];
                BigDecimal sodaMl = (BigDecimal) row[3];

                machineWiseIotUsage.add(MachineConsumptionDto.builder()
                        .machineCode(mCode)
                        .machineName(mName)
                        .syrupUsageMl(syrupMl)
                        .rawSodaUsageMl(sodaMl)
                        .build());
            }
        }

        BigDecimal manualQuantityTotal = BigDecimal.ZERO;
        List<ManualFlavourSalesSummaryDto> manualFlavourTotals = new ArrayList<>();

        if ("MANUAL".equals(normalizedSource) || "ALL".equals(normalizedSource)) {
            manualQuantityTotal = manualDailySalesRepository.sumQuantity(
                    effectiveFranchiseId, machineId, productId, null, fromDate, toDate);

            List<Object[]> manualRows = manualDailySalesRepository.findFlavourSalesTotals(
                    effectiveFranchiseId, machineId, productId, fromDate, toDate);

            for (Object[] row : manualRows) {
                UUID pId = (UUID) row[0];
                String itemCode = (String) row[1];
                String flavourName = (String) row[2];
                BigDecimal glasses = (BigDecimal) row[3];
                BigDecimal ml = (BigDecimal) row[4];

                manualFlavourTotals.add(ManualFlavourSalesSummaryDto.builder()
                        .productId(pId)
                        .itemCode(itemCode)
                        .flavourName(flavourName)
                        .glassesTotal(glasses)
                        .mlTotal(ml)
                        .build());
            }
        }

        String notes = "IoT machine telemetry is recorded in ML. Manual sales entries are tracked in Glasses/ML. " +
                       "Values are maintained separately and retail sales monetary revenue is not calculated.";

        return ConsumptionSummaryResponseDto.builder()
                .totalSyrupUsageMl(totalSyrupUsageMl != null ? totalSyrupUsageMl : BigDecimal.ZERO)
                .totalRawSodaUsageMl(totalRawSodaUsageMl != null ? totalRawSodaUsageMl : BigDecimal.ZERO)
                .manualQuantityTotal(manualQuantityTotal != null ? manualQuantityTotal : BigDecimal.ZERO)
                .flavourWiseIotUsage(flavourWiseIotUsage)
                .machineWiseIotUsage(machineWiseIotUsage)
                .manualFlavourSalesTotals(manualFlavourTotals)
                .sourceNotes(notes)
                .build();
    }

    /**
     * Streams consumption CSV report with proper escaping and formula injection protection.
     */
    @Transactional(readOnly = true)
    public void exportConsumptionReportCsv(
            UUID requestedFranchiseId,
            UUID machineId,
            UUID productId,
            LocalDate fromDate,
            LocalDate toDate,
            String source,
            PrintWriter writer,
            UserPrincipal principal
    ) {
        UUID effectiveFranchiseId = resolveEffectiveFranchiseId(requestedFranchiseId, principal);
        String normalizedSource = normalizeSource(source);

        Instant fromDateTime = fromDate != null ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant() : null;

        boolean includeIot = "IOT".equals(normalizedSource) || "ALL".equals(normalizedSource);
        boolean includeManual = "MANUAL".equals(normalizedSource) || "ALL".equals(normalizedSource);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        if (includeIot) {
            // Write IoT header
            writer.print(CsvExportUtil.formatRow(Arrays.asList(
                    "Source", "Event ID", "Event Time", "Franchise", "Machine Code",
                    "Device ID", "Drink Name", "Item Type", "Item Code", "Flavour Name",
                    "Line Number", "Quantity ML", "Event Status"
            )));

            List<IotEventItem> iotItems = iotEventItemRepository.findReportItems(
                    effectiveFranchiseId, machineId, productId, fromDateTime, toDateTime);

            for (IotEventItem item : iotItems) {
                String franchiseName = item.getIotEvent().getFranchise() != null ? item.getIotEvent().getFranchise().getBusinessName() : "";
                String machineCode = item.getIotEvent().getMachine() != null ? item.getIotEvent().getMachine().getMachineCode() : "";
                String deviceId = item.getIotEvent().getMachine() != null ? item.getIotEvent().getMachine().getDeviceId() : "";
                String flavour = item.getProduct() != null ? item.getProduct().getFlavourName() : item.getItemName();

                writer.print(CsvExportUtil.formatRow(Arrays.asList(
                        "IOT",
                        item.getIotEvent().getExternalEventId(),
                        dtf.format(item.getIotEvent().getEventTime()),
                        franchiseName,
                        machineCode,
                        deviceId,
                        item.getIotEvent().getDrinkName() != null ? item.getIotEvent().getDrinkName() : "",
                        item.getItemType().name(),
                        item.getItemCode(),
                        flavour != null ? flavour : "",
                        item.getLineNo() != null ? item.getLineNo().toString() : "",
                        item.getQuantityMl() != null ? item.getQuantityMl().toString() : "0.000",
                        item.getIotEvent().getEventStatus().name()
                )));
            }
        }

        if (includeIot && includeManual) {
            writer.print("\r\n"); // Blank line separator between sections in unified export
        }

        if (includeManual) {
            // Write Manual Sales header
            writer.print(CsvExportUtil.formatRow(Arrays.asList(
                    "Source", "Sales Date", "Franchise", "Machine Code", "Flavour Name",
                    "Quantity", "Quantity Unit", "Notes", "Entered By", "Created At"
            )));

            List<ManualDailyFlavourSales> manualSales = manualDailySalesRepository.findReportSales(
                    effectiveFranchiseId, machineId, productId, fromDate, toDate);

            for (ManualDailyFlavourSales sale : manualSales) {
                String franchiseName = sale.getFranchise() != null ? sale.getFranchise().getBusinessName() : "";
                String machineCode = sale.getMachine() != null ? sale.getMachine().getMachineCode() : "";
                String flavourName = sale.getProduct() != null ? sale.getProduct().getFlavourName() : "";

                writer.print(CsvExportUtil.formatRow(Arrays.asList(
                        "MANUAL",
                        sale.getSalesDate().toString(),
                        franchiseName,
                        machineCode,
                        flavourName,
                        sale.getQuantity() != null ? sale.getQuantity().toString() : "0.000",
                        sale.getQuantityUnit().name(),
                        sale.getNotes() != null ? sale.getNotes() : "",
                        sale.getCreatedBy() != null ? sale.getCreatedBy().toString() : "",
                        dtf.format(sale.getCreatedAt())
                )));
            }
        }
    }

    /**
     * Generates consumption CSV report bytes.
     */
    @Transactional(readOnly = true)
    public byte[] generateConsumptionReportCsv(
            UUID requestedFranchiseId,
            UUID machineId,
            UUID productId,
            LocalDate fromDate,
            LocalDate toDate,
            String source,
            UserPrincipal principal
    ) {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new java.io.OutputStreamWriter(baos, java.nio.charset.StandardCharsets.UTF_8))) {
            exportConsumptionReportCsv(requestedFranchiseId, machineId, productId, fromDate, toDate, source, writer, principal);
            writer.flush();
        }
        return baos.toByteArray();
    }

    private UUID resolveEffectiveFranchiseId(UUID requestedFranchiseId, UserPrincipal principal) {
        if (principal.getRole() == UserRole.FRANCHISE_OWNER) {
            if (principal.getFranchiseId() == null) {
                throw new AuthException("Franchise Owner is not linked to an active franchise");
            }
            return principal.getFranchiseId();
        }
        return requestedFranchiseId;
    }

    private String normalizeSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            return "ALL";
        }
        String upper = source.trim().toUpperCase();
        if ("IOT".equals(upper) || "MANUAL".equals(upper)) {
            return upper;
        }
        return "ALL";
    }
}
