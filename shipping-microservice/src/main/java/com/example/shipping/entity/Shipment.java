package com.example.shipping.entity;

import com.example.shipping.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "shipment_id")
    private String shipmentId;

    @Column(name = "quote_id", nullable = false)
    private String quoteId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "branch_id", nullable = false)
    private String branchId;

    @Column(name = "destination_line1", nullable = false)
    private String destinationLine1;

    @Column(name = "destination_city", nullable = false)
    private String destinationCity;

    @Column(name = "destination_area", nullable = false)
    private String destinationArea;

    @Column(name = "destination_postal_code")
    private String destinationPostalCode;

    @Column(name = "destination_country", nullable = false)
    private String destinationCountry;

    @Column(name = "shipping_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal shippingCost;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ShipmentStatus.CONFIRMED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
