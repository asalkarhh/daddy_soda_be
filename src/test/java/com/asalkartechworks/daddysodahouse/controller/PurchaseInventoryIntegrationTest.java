package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
public class PurchaseInventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Test
    void testGetStockMovements() {
        var page = inventoryService.getStockMovements(
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );
        assertNotNull(page);
    }

    @Test
    void testGetStockMovementsForCocaCola() {
        UUID cocaColaId = UUID.fromString("8e1e3f2a-4438-4cd0-baa9-f7a216e629ff");
        var page = inventoryService.getStockMovements(
                cocaColaId,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );
        assertNotNull(page);
        assertTrue(page.getTotalElements() > 0, "Coca Cola should have movements in the database");
    }
}
