package com.asalkartechworks.daddysodahouse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for public website enquiry and WhatsApp redirection.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.enquiry")
public class EnquiryProperties {

    /**
     * WhatsApp enquiry recipient number in international format without plus sign
     * (e.g. 919876543210 for an Indian number).
     */
    private String whatsappNumber = "919876543210";
}
