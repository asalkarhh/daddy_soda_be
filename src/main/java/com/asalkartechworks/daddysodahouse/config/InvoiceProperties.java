package com.asalkartechworks.daddysodahouse.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration properties for billing, GST, company details, and invoice PDF storage.
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "app.invoice")
public class InvoiceProperties {

    private String companyName = "Daddy Soda House Pvt Ltd";
    private String companyAddress = "Shop 12, Phoenix Marketcity, Viman Nagar, Pune, Maharashtra 411014";
    private String companyMobile = "9999999999";
    private String companyEmail = "accounts@daddysoda.com";
    private String companyGstin = "27AABCS1429B1Z1";
    private BigDecimal defaultGstRatePercent = new BigDecimal("18.00");
    private String storagePath = "./storage/invoices";

    @PostConstruct
    public void initStorageDirectory() {
        try {
            Path path = Paths.get(storagePath).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created invoice PDF storage directory: {}", path);
            } else {
                log.info("Invoice PDF storage directory initialized at: {}", path);
            }
        } catch (IOException e) {
            log.error("Failed to create invoice PDF storage directory: {}", storagePath, e);
        }
    }
}
