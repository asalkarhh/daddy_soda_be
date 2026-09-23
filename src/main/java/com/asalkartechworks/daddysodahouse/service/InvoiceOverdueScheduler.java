package com.asalkartechworks.daddysodahouse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to refresh overdue status for invoices past their credit due dates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceOverdueScheduler {

    private final InvoiceService invoiceService;

    /**
     * Executes daily at 12:05 AM server time.
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void runDailyOverdueStatusRefresh() {
        log.info("Starting daily overdue invoice status check...");
        try {
            int updatedCount = invoiceService.refreshOverdueInvoices();
            log.info("Completed daily overdue status check. Invoices updated: {}", updatedCount);
        } catch (Exception e) {
            log.error("Error running daily overdue invoice refresh: {}", e.getMessage(), e);
        }
    }
}
