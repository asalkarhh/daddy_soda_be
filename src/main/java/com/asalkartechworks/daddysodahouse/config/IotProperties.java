package com.asalkartechworks.daddysodahouse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for IoT Soda Machine telemetry integration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.iot")
public class IotProperties {

    /**
     * Mandatory secret API key for machine-to-server ingestion requests (X-API-KEY).
     */
    private String apiKey;

    /**
     * Inactivity threshold in minutes after which a machine is considered OFFLINE (default: 30 minutes).
     */
    private int offlineThresholdMinutes = 30;
}
