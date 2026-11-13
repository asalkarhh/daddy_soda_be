package com.asalkartechworks.daddysodahouse.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration properties for Outlet file attachments, agreements, and storage.
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "app.agreement")
public class OutletProperties {

    private String storagePath = "./storage/agreements";

    @PostConstruct
    public void initStorageDirectory() {
        try {
            Path path = Paths.get(storagePath).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created outlet agreement storage directory: {}", path);
            } else {
                log.info("Outlet agreement storage directory initialized at: {}", path);
            }
        } catch (IOException e) {
            log.error("Failed to create outlet agreement storage directory: {}", storagePath, e);
        }
    }
}
