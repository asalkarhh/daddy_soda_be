package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.Enquiry;
import com.asalkartechworks.daddysodahouse.entity.EnquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, UUID>, JpaSpecificationExecutor<Enquiry> {

    Optional<Enquiry> findByEnquiryNumber(String enquiryNumber);

    @Query("SELECT MAX(e.enquiryNumber) FROM Enquiry e WHERE e.enquiryNumber LIKE CONCAT(:prefix, '%')")
    Optional<String> findMaxEnquiryNumberWithPrefix(@Param("prefix") String prefix);

    long countByStatus(EnquiryStatus status);

    @Query("""
        SELECT e FROM Enquiry e
        WHERE (:status IS NULL OR e.status = :status)
          AND (:search IS NULL OR (
                LOWER(e.fullName) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                OR e.mobile LIKE CONCAT('%', CAST(:search AS String), '%')
                OR LOWER(e.city) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                OR LOWER(e.enquiryNumber) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
          ))
          AND (:fromDate IS NULL OR e.createdAt >= :fromDate)
          AND (:toDate IS NULL OR e.createdAt <= :toDate)
    """)
    Page<Enquiry> searchEnquiries(
            @Param("status") EnquiryStatus status,
            @Param("search") String search,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
}
