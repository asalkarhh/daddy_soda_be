package com.asalkartechworks.daddysodahouse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "product")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", length = 30, nullable = false)
    private StockMovementType movementType;

    @Builder.Default
    @Column(name = "quantity_in", precision = 14, scale = 3, nullable = false)
    private BigDecimal quantityIn = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "quantity_out", precision = 14, scale = 3, nullable = false)
    private BigDecimal quantityOut = BigDecimal.ZERO;

    @Column(name = "balance_after", precision = 14, scale = 3, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "reference_type", length = 50, nullable = false)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "movement_at", nullable = false)
    private Instant movementAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;
}
