package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, UUID> {

    List<SalesOrderItem> findBySalesOrderId(UUID salesOrderId);
}
