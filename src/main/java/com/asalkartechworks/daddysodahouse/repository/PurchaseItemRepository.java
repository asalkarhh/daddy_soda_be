package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, UUID> {

    List<PurchaseItem> findByPurchaseEntryId(UUID purchaseEntryId);
}
