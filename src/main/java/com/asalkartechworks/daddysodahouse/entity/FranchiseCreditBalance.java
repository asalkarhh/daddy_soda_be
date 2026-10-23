package com.asalkartechworks.daddysodahouse.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "franchise_credit_balances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FranchiseCreditBalance {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "franchise_id", nullable = false, unique = true)
    private Franchise franchise;
    @Column(name = "balance_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default private BigDecimal balanceAmount = BigDecimal.ZERO;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
