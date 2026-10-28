package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.config.InvoiceProperties;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.Invoice;
import com.asalkartechworks.daddysodahouse.entity.InvoiceItem;
import com.asalkartechworks.daddysodahouse.entity.InvoiceType;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating and persisting server-side invoice PDFs using OpenPDF.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final InvoiceProperties invoiceProperties;

    private static final Color PRIMARY_COLOR = new Color(30, 41, 59);
    private static final Color HEADER_BG = new Color(241, 245, 249);
    private static final Color BORDER_COLOR = new Color(203, 213, 225);
    private static final Color MUTED_TEXT = new Color(100, 116, 139);

    /**
     * Generate PDF byte array for an invoice.
     */
    public byte[] generatePdfBytes(Invoice invoice) throws DocumentException {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        buildInvoiceDocument(document, invoice);

        document.close();
        return baos.toByteArray();
    }

    /**
     * Generate PDF and save it to the configured storage path.
     * Returns the absolute path of the generated PDF.
     */
    public String generateAndSavePdf(Invoice invoice) {
        try {
            byte[] pdfBytes = generatePdfBytes(invoice);
            Path storageDir = Paths.get(invoiceProperties.getStoragePath()).toAbsolutePath().normalize();
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }

            String filename = invoice.getInvoiceNumber() + ".pdf";
            Path targetFile = storageDir.resolve(filename);

            try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
                fos.write(pdfBytes);
            }

            log.info("Saved invoice PDF for {} at {}", invoice.getInvoiceNumber(), targetFile);
            return targetFile.toString();
        } catch (Exception e) {
            log.error("Failed to generate or save invoice PDF for {}: {}", invoice.getInvoiceNumber(), e.getMessage(), e);
            throw new RuntimeException("Could not generate invoice PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Read the invoice PDF bytes from storage; if missing on disk, regenerate and save.
     */
    public byte[] getOrRegeneratePdfBytes(Invoice invoice) {
        try {
            Path storageDir = Paths.get(invoiceProperties.getStoragePath()).toAbsolutePath().normalize();
            String filename = invoice.getInvoiceNumber() + ".pdf";
            Path targetFile = storageDir.resolve(filename);

            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
            byte[] generated = generatePdfBytes(invoice);
            Files.write(targetFile, generated);
            return generated;
        } catch (Exception e) {
            log.error("Failed to read or regenerate PDF for {}: {}", invoice.getInvoiceNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to load invoice PDF: " + e.getMessage(), e);
        }
    }

    private void buildInvoiceDocument(Document document, Invoice invoice) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY_COLOR);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_COLOR);
        Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 9, PRIMARY_COLOR);
        Font mutedFont = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED_TEXT);

        // Header Table (Company info left, Invoice type/number right)
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60f, 40f});

        // Company Details (Left)
        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(PdfPCell.NO_BORDER);
        companyCell.addElement(new Paragraph(invoiceProperties.getCompanyName(), titleFont));
        companyCell.addElement(new Paragraph(invoiceProperties.getCompanyAddress(), regularFont));
        companyCell.addElement(new Paragraph("Mobile: " + invoiceProperties.getCompanyMobile() + " | Email: " + invoiceProperties.getCompanyEmail(), regularFont));
        if (invoice.getInvoiceType() == InvoiceType.GST && StringUtils.hasText(invoiceProperties.getCompanyGstin())) {
            companyCell.addElement(new Paragraph("Company GSTIN: " + invoiceProperties.getCompanyGstin(), boldFont));
        }
        headerTable.addCell(companyCell);

        // Invoice Meta (Right)
        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(PdfPCell.NO_BORDER);
        metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        String billTypeHeader = invoice.getInvoiceType() == InvoiceType.GST ? "TAX INVOICE (GST)" : "RETAIL INVOICE (NORMAL)";
        Paragraph pBillType = new Paragraph(billTypeHeader, subTitleFont);
        pBillType.setAlignment(Element.ALIGN_RIGHT);
        metaCell.addElement(pBillType);

        Paragraph pInvNo = new Paragraph("Invoice No: " + invoice.getInvoiceNumber(), boldFont);
        pInvNo.setAlignment(Element.ALIGN_RIGHT);
        metaCell.addElement(pInvNo);

        Paragraph pDate = new Paragraph("Invoice Date: " + invoice.getInvoiceDate(), regularFont);
        pDate.setAlignment(Element.ALIGN_RIGHT);
        metaCell.addElement(pDate);

        Paragraph pOrder = new Paragraph("Sales Order: " + invoice.getSalesOrder().getOrderNumber(), regularFont);
        pOrder.setAlignment(Element.ALIGN_RIGHT);
        metaCell.addElement(pOrder);

        Paragraph pTerm = new Paragraph("Payment Term: " + invoice.getPaymentTerm(), regularFont);
        pTerm.setAlignment(Element.ALIGN_RIGHT);
        metaCell.addElement(pTerm);

        if (invoice.getDueDate() != null) {
            Paragraph pDue = new Paragraph("Due Date: " + invoice.getDueDate(), boldFont);
            pDue.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(pDue);
        }

        headerTable.addCell(metaCell);
        document.add(headerTable);

        document.add(new Paragraph(" "));

        // Customer Details (Billed To)
        Franchise franchise = invoice.getSalesOrder().getFranchise();
        PdfPTable billToTable = new PdfPTable(1);
        billToTable.setWidthPercentage(100);

        PdfPCell billToCell = new PdfPCell();
        billToCell.setBackgroundColor(HEADER_BG);
        billToCell.setBorderColor(BORDER_COLOR);
        billToCell.setPadding(8);
        billToCell.addElement(new Paragraph("BILLED TO (FRANCHISE):", boldFont));
        billToCell.addElement(new Paragraph(franchise.getBusinessName() + " (" + franchise.getFranchiseCode() + ")", boldFont));
        billToCell.addElement(new Paragraph("Proprietor / Contact: " + franchise.getOwnerName() + " | Mobile: " + franchise.getMobile(), regularFont));
        if (StringUtils.hasText(franchise.getAddress())) {
            billToCell.addElement(new Paragraph("Address: " + franchise.getAddress() + ", " + franchise.getCity() + ", " + franchise.getState() + " - " + franchise.getPincode(), regularFont));
        }
        if (StringUtils.hasText(franchise.getFssaiNumber())) {
            billToCell.addElement(new Paragraph("FSSAI Reg: " + franchise.getFssaiNumber(), regularFont));
        }
        billToTable.addCell(billToCell);
        document.add(billToTable);

        document.add(new Paragraph(" "));

        // Items Table
        PdfPTable itemsTable = new PdfPTable(7);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{6f, 18f, 32f, 12f, 10f, 11f, 11f});

        addTableHeader(itemsTable, "#", boldFont);
        addTableHeader(itemsTable, "Item Code", boldFont);
        addTableHeader(itemsTable, "Product / Flavour", boldFont);
        addTableHeader(itemsTable, "Bottle Size", boldFont);
        addTableHeader(itemsTable, "Qty", boldFont);
        addTableHeader(itemsTable, "Rate (Rs.)", boldFont);
        addTableHeader(itemsTable, "Total (Rs.)", boldFont);

        int seq = 1;
        for (InvoiceItem item : invoice.getItems()) {
            addTableCell(itemsTable, String.valueOf(seq++), regularFont, Element.ALIGN_CENTER);
            addTableCell(itemsTable, item.getItemCode(), regularFont, Element.ALIGN_LEFT);
            addTableCell(itemsTable, item.getFlavourName(), regularFont, Element.ALIGN_LEFT);
            addTableCell(itemsTable, item.getBottleSizeLitre() != null ? item.getBottleSizeLitre() + " L" : "-", regularFont, Element.ALIGN_CENTER);
            addTableCell(itemsTable, item.getQuantity() != null ? item.getQuantity().toString() : "0", regularFont, Element.ALIGN_RIGHT);
            addTableCell(itemsTable, formatDecimal(item.getUnitPrice()), regularFont, Element.ALIGN_RIGHT);
            addTableCell(itemsTable, formatDecimal(item.getLineTotal()), regularFont, Element.ALIGN_RIGHT);
        }
        document.add(itemsTable);

        document.add(new Paragraph(" "));

        // Totals & Status Table
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{55f, 45f});

        // Left note cell
        PdfPCell noteCell = new PdfPCell();
        noteCell.setBorder(PdfPCell.NO_BORDER);
        noteCell.setPadding(6);
        if (StringUtils.hasText(invoice.getNotes())) {
            noteCell.addElement(new Paragraph("Notes / Instructions:", boldFont));
            noteCell.addElement(new Paragraph(invoice.getNotes(), mutedFont));
        }
        noteCell.addElement(new Paragraph("Payment Status: " + invoice.getPaymentStatus(), boldFont));
        summaryTable.addCell(noteCell);

        // Right totals table
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(100);
        totalsTable.setWidths(new float[]{50f, 50f});

        addTotalRow(totalsTable, "Subtotal:", formatAmount(invoice.getSubtotalAmount()), regularFont, regularFont);

        if (invoice.getInvoiceType() == InvoiceType.GST) {
            String rateStr = invoice.getGstRatePercent() != null ? invoice.getGstRatePercent().toString() : "0";
            addTotalRow(totalsTable, "GST (" + rateStr + "%):", formatAmount(invoice.getGstAmount()), regularFont, regularFont);
        }

        addTotalRow(totalsTable, "Grand Total:", formatAmount(invoice.getTotalAmount()), boldFont, boldFont);
        if (invoice.getCreditAppliedAmount() != null && invoice.getCreditAppliedAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(totalsTable, "Credit Applied:", formatAmount(invoice.getCreditAppliedAmount()), regularFont, regularFont);
        }
        addTotalRow(totalsTable, "Amount Paid:", formatAmount(invoice.getPaidAmount()), regularFont, regularFont);
        addTotalRow(totalsTable, "Balance Due:", formatAmount(invoice.getBalanceAmount()), boldFont, boldFont);

        PdfPCell totalsWrapper = new PdfPCell(totalsTable);
        totalsWrapper.setBorderColor(BORDER_COLOR);
        totalsWrapper.setPadding(4);
        summaryTable.addCell(totalsWrapper);

        document.add(summaryTable);

        // Footer
        document.add(new Paragraph(" "));
        String generatedAt = invoice.getPdfGeneratedAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(invoice.getPdfGeneratedAt())
                : DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now());
        Paragraph footer = new Paragraph("Generated on: " + generatedAt + " | This is a computer-generated invoice.", mutedFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, labelFont));
        cellLabel.setBorder(PdfPCell.NO_BORDER);
        cellLabel.setPadding(3);
        table.addCell(cellLabel);

        PdfPCell cellVal = new PdfPCell(new Phrase(value, valFont));
        cellVal.setBorder(PdfPCell.NO_BORDER);
        cellVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellVal.setPadding(3);
        table.addCell(cellVal);
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("Rs. %.2f", amount != null ? amount : BigDecimal.ZERO);
    }

    private String formatDecimal(BigDecimal amount) {
        return String.format("%.2f", amount != null ? amount : BigDecimal.ZERO);
    }
}
