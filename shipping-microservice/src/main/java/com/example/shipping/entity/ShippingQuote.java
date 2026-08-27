package com.example.shipping.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_quotes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "quote_id")
    private String quoteId;

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

    @Column(name = "total_items")
    private Integer totalItems;

    @Column(name = "total_weight_kg", precision = 10, scale = 2)
    private BigDecimal totalWeightKg;

    @Column(name = "shipping_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal shippingCost;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "is_used")
    private Boolean isUsed;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = createdAt.plusHours(24);
        if (isUsed == null) {
            isUsed = false;
        }
        if (currency == null) {
            currency = "LKR";
        }
    }

}
