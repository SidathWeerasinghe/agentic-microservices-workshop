package com.example.shipping.service;

import com.example.shipping.dto.*;
import com.example.shipping.entity.Shipment;
import com.example.shipping.entity.ShippingQuote;
import com.example.shipping.enums.ShipmentStatus;
import com.example.shipping.exception.QuoteExpiredException;
import com.example.shipping.exception.QuoteNotFoundException;
import com.example.shipping.repository.ShipmentRepository;
import com.example.shipping.repository.ShippingQuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {

    private final ShippingQuoteRepository quoteRepository;
    private final ShipmentRepository shipmentRepository;

    private static final BigDecimal BASE_RATE = new BigDecimal("100.00");
    private static final BigDecimal PER_KG_RATE = new BigDecimal("50.00");
    private static final BigDecimal PER_ITEM_RATE = new BigDecimal("25.00");

    // Origin city of each branch (drives the distance-based shipping surcharge).
    private static final Map<String, String> BRANCH_CITY = Map.of(
            "BRANCH-COL", "colombo",
            "BRANCH-GAM", "gampaha",
            "BRANCH-KDY", "kandy",
            "BRANCH-JAF", "jaffna");

    // Approximate road distance (km) between key Sri Lankan cities. Symmetric lookup.
    private static final Map<String, Integer> DISTANCE_KM = Map.ofEntries(
            Map.entry("colombo|gampaha", 30),
            Map.entry("colombo|kandy", 115),
            Map.entry("colombo|jaffna", 400),
            Map.entry("gampaha|kandy", 90),
            Map.entry("gampaha|jaffna", 380),
            Map.entry("kandy|jaffna", 290));

    // Distance unknown but domestic: assume a mid-range haul.
    private static final int DEFAULT_DOMESTIC_KM = 150;

    @Transactional
    public ShippingQuoteResponse calculateShippingQuote(ShippingQuoteRequest request) {
        log.info("Calculating shipping quote for branch: {} to {}, {}",
                request.getBranchId(),
                request.getDestinationAddress().getCity(),
                request.getDestinationAddress().getCountry());

        BigDecimal totalWeight = request.getItems().stream()
                .filter(item -> item.getWeightKg() != null)
                .map(item -> item.getWeightKg().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = request.getItems().stream()
                .mapToInt(ShippingItem::getQuantity)
                .sum();

        BigDecimal shippingCost = calculateCost(totalWeight, totalItems,
                request.getBranchId(), request.getDestinationAddress());

        Address dest = request.getDestinationAddress();
        ShippingQuote quote = ShippingQuote.builder()
                .branchId(request.getBranchId())
                .destinationLine1(dest.getLine1())
                .destinationCity(dest.getCity())
                .destinationArea(dest.getArea())
                .destinationPostalCode(dest.getPostalCode())
                .destinationCountry(dest.getCountry())
                .totalItems(totalItems)
                .totalWeightKg(totalWeight)
                .shippingCost(shippingCost)
                .currency("LKR")
                .build();

        quote = quoteRepository.save(quote);

        log.info("Created shipping quote: {} with cost: {} LKR", quote.getQuoteId(), shippingCost);

        return ShippingQuoteResponse.builder()
                .quoteId(quote.getQuoteId())
                .shippingCost(quote.getShippingCost())
                .currency(quote.getCurrency())
                .build();
    }

    @Transactional
    public ShippingConfirmationResponse confirmShipping(ShippingConfirmationRequest request) {
        log.info("Confirming shipping for quote: {}, order: {}", request.getQuoteId(), request.getOrderId());

        ShippingQuote quote = quoteRepository.findByQuoteIdAndIsUsedFalse(request.getQuoteId())
                .orElseThrow(() -> new QuoteNotFoundException(
                        "Quote not found or already used: " + request.getQuoteId()));

        if (quote.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new QuoteExpiredException("Quote has expired: " + request.getQuoteId());
        }

        quote.setIsUsed(true);
        quoteRepository.save(quote);

        Shipment shipment = Shipment.builder()
                .quoteId(quote.getQuoteId())
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .branchId(quote.getBranchId())
                .destinationLine1(quote.getDestinationLine1())
                .destinationCity(quote.getDestinationCity())
                .destinationArea(quote.getDestinationArea())
                .destinationPostalCode(quote.getDestinationPostalCode())
                .destinationCountry(quote.getDestinationCountry())
                .shippingCost(quote.getShippingCost())
                .currency(quote.getCurrency())
                .status(ShipmentStatus.CONFIRMED)
                .build();

        shipment = shipmentRepository.save(shipment);

        log.info("Shipping confirmed with shipment ID: {}", shipment.getShipmentId());

        return ShippingConfirmationResponse.builder()
                .shipmentId(shipment.getShipmentId())
                .status(shipment.getStatus())
                .build();
    }

    private BigDecimal calculateCost(BigDecimal totalWeight, int totalItems,
                                     String branchId, Address destination) {
        BigDecimal cost = BASE_RATE;

        cost = cost.add(totalWeight.multiply(PER_KG_RATE));
        cost = cost.add(BigDecimal.valueOf(totalItems).multiply(PER_ITEM_RATE));

        // Distance surcharge: the further the goods travel from the origin branch
        // to the destination city, the more it costs to ship them.
        int distanceKm = estimateDistanceKm(branchId, destination.getCity());
        BigDecimal distanceSurcharge = distanceZoneSurcharge(distanceKm);
        cost = cost.add(distanceSurcharge);
        log.info("Shipping from branch {} to {} (~{} km) -> distance surcharge {} LKR",
                branchId, destination.getCity(), distanceKm, distanceSurcharge);

        if (!"Sri Lanka".equalsIgnoreCase(destination.getCountry()) &&
            !"LK".equalsIgnoreCase(destination.getCountry())) {
            cost = cost.multiply(new BigDecimal("2.5"));
        }

        return cost.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** Estimate road distance (km) from the origin branch's city to the destination city. */
    private int estimateDistanceKm(String branchId, String destinationCity) {
        String origin = BRANCH_CITY.getOrDefault(branchId == null ? "" : branchId.toUpperCase(), null);
        String dest = destinationCity == null ? "" : destinationCity.trim().toLowerCase();
        if (origin == null) {
            return DEFAULT_DOMESTIC_KM;
        }
        // Match the destination against a known city name (handles "Colombo 03" -> colombo).
        String matchedDest = BRANCH_CITY.values().stream()
                .filter(dest::contains)
                .findFirst()
                .orElse(dest);
        if (origin.equals(matchedDest)) {
            return 0; // same city = local delivery
        }
        Integer km = DISTANCE_KM.get(origin + "|" + matchedDest);
        if (km == null) {
            km = DISTANCE_KM.get(matchedDest + "|" + origin); // symmetric
        }
        return km != null ? km : DEFAULT_DOMESTIC_KM;
    }

    /** Convert a distance into a tiered shipping surcharge (LKR). */
    private BigDecimal distanceZoneSurcharge(int km) {
        if (km <= 0) return new BigDecimal("500.00");     // local
        if (km <= 50) return new BigDecimal("1500.00");   // nearby
        if (km <= 150) return new BigDecimal("5000.00");  // regional
        if (km <= 300) return new BigDecimal("12000.00"); // long haul
        return new BigDecimal("20000.00");                // remote
    }

}
