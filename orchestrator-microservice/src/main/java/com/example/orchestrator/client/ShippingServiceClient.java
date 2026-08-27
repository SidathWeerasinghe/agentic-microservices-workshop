package com.example.orchestrator.client;

import com.example.orchestrator.dto.OrderItem;
import com.example.orchestrator.dto.ShippingAddress;
import com.example.orchestrator.dto.ShippingConfirmationResult;
import com.example.orchestrator.dto.ShippingQuoteResult;
import com.example.orchestrator.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ShippingServiceClient {

    private final WebClient webClient;

    public ShippingServiceClient(@Qualifier("shippingWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public ShippingQuoteResult calculateShippingQuote(String branchId, ShippingAddress address, List<OrderItem> items) {
        log.info("Calculating shipping quote from branch: {} to {}, {}", branchId, address.getCity(), address.getCountry());

        try {
            Map<String, Object> destinationAddress = new HashMap<>();
            destinationAddress.put("line1", address.getLine1());
            destinationAddress.put("city", address.getCity());
            destinationAddress.put("area", address.getArea());
            destinationAddress.put("postalCode", address.getPostalCode());
            destinationAddress.put("country", address.getCountry());

            List<Map<String, Object>> shippingItems = items.stream()
                    .map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("itemId", item.getItemId());
                        itemMap.put("quantity", item.getQuantity());
                        if (item.getWeightKg() != null) {
                            itemMap.put("weightKg", item.getWeightKg());
                        }
                        return itemMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> request = new HashMap<>();
            request.put("branchId", branchId);
            request.put("destinationAddress", destinationAddress);
            request.put("items", shippingItems);

            Map<String, Object> response = webClient.post()
                    .uri("/quotes")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            Mono.error(new ServiceException("Shipping quote calculation failed")))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            Mono.error(new ServiceException("Shipping service unavailable")))
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                BigDecimal shippingCost = response.get("shippingCost") instanceof Number ?
                        new BigDecimal(response.get("shippingCost").toString()) : BigDecimal.ZERO;

                return ShippingQuoteResult.builder()
                        .quoteId((String) response.get("quoteId"))
                        .shippingCost(shippingCost)
                        .currency((String) response.get("currency"))
                        .build();
            }
            throw new ServiceException("Empty response from shipping service");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calculating shipping quote: {}", e.getMessage());
            throw new ServiceException("Failed to calculate shipping quote: " + e.getMessage());
        }
    }

    public ShippingConfirmationResult confirmShipping(String quoteId, String orderId, String customerId) {
        log.info("Confirming shipping for quote: {}, order: {}", quoteId, orderId);

        try {
            Map<String, Object> request = Map.of(
                    "quoteId", quoteId,
                    "orderId", orderId,
                    "customerId", customerId
            );

            Map<String, Object> response = webClient.post()
                    .uri("/confirmations")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            Mono.error(new ServiceException("Shipping confirmation failed")))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            Mono.error(new ServiceException("Shipping service unavailable")))
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                return ShippingConfirmationResult.builder()
                        .shipmentId((String) response.get("shipmentId"))
                        .status((String) response.get("status"))
                        .build();
            }
            throw new ServiceException("Empty response from shipping service");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error confirming shipping: {}", e.getMessage());
            throw new ServiceException("Failed to confirm shipping: " + e.getMessage());
        }
    }

}
