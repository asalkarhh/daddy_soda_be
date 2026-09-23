package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.PaymentCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentCollectionRepository extends JpaRepository<PaymentCollection, UUID> {

    List<PaymentCollection> findByInvoiceIdOrderByPaymentDateDescCreatedAtDesc(UUID invoiceId);
}
