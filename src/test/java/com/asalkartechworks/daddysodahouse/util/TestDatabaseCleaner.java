package com.asalkartechworks.daddysodahouse.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Utility component for integration tests to reset the shared test database
 * cleanly in exact foreign-key reverse dependency order.
 */
@Component
@RequiredArgsConstructor
public class TestDatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

    public void cleanAll() {
        jdbcTemplate.execute("DELETE FROM enquiries");
        jdbcTemplate.execute("DELETE FROM payment_collections");
        jdbcTemplate.execute("DELETE FROM invoice_items");
        jdbcTemplate.execute("DELETE FROM invoices");
        jdbcTemplate.execute("DELETE FROM stock_movements");
        jdbcTemplate.execute("DELETE FROM sales_order_items");
        jdbcTemplate.execute("DELETE FROM sales_orders");
        jdbcTemplate.execute("DELETE FROM purchase_items");
        jdbcTemplate.execute("DELETE FROM purchase_entries");
        jdbcTemplate.execute("DELETE FROM inventory_stock");
        jdbcTemplate.execute("DELETE FROM machines");
        jdbcTemplate.execute("DELETE FROM outlets");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM franchises");
        jdbcTemplate.execute("DELETE FROM products");
        jdbcTemplate.execute("DELETE FROM suppliers");
    }
}
