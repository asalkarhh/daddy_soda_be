package com.asalkartechworks.daddysodahouse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Item entity representing an ingredient line (syrup flavour or raw soda) dispensed in an IoT event.
 */
@Entity
@Table(name = "iot_event_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"iotEvent", "product"})
public class IotEventItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iot_event_id", nullable = false)
    private IotEvent iotEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 30, nullable = false)
    private IotItemType itemType;

    @Column(name = "item_code", length = 50, nullable = false)
    private String itemCode;

    @Column(name = "item_name", length = 150)
    private String itemName;

    @Column(name = "line_no")
    private Integer lineNo;

    @Column(name = "quantity_ml", precision = 14, scale = 3, nullable = false)
    private BigDecimal quantityMl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
