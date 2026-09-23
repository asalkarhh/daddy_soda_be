package com.asalkartechworks.daddysodahouse.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * IoT Event entity representing a telemetry record (drink dispense or heartbeat)
 * received from a soda machine.
 */
@Entity
@Table(name = "iot_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"items", "machine", "franchise"})
public class IotEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "external_event_id", length = 100, nullable = false, unique = true)
    private String externalEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "franchise_id", nullable = false)
    private Franchise franchise;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 30, nullable = false)
    private IotEventType eventType;

    @Column(name = "drink_name", length = 150)
    private String drinkName;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", length = 30, nullable = false)
    private IotEventStatus eventStatus;

    @Builder.Default
    @Column(name = "syrup_quantity_ml", precision = 14, scale = 3, nullable = false)
    private BigDecimal syrupQuantityMl = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "raw_soda_quantity_ml", precision = 14, scale = 3, nullable = false)
    private BigDecimal rawSodaQuantityMl = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false)
    private JsonNode rawPayload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "iotEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IotEventItem> items = new ArrayList<>();

    public void addItem(IotEventItem item) {
        items.add(item);
        item.setIotEvent(this);
    }
}
