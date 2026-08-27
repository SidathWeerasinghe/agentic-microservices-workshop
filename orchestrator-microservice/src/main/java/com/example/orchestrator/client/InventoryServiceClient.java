package com.example.orchestrator.client;

import com.example.orchestrator.dto.ProductionOrderResult;
import com.example.orchestrator.dto.StockCheckResult;
import com.example.orchestrator.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.math.BigDecimal;

@Component
@Slf4j
public class InventoryServiceClient {

    private final WebClient webClient;

    public InventoryServiceClient(@Qualifier("inventoryWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public StockCheckResult checkStock(String itemId, String branchId) {
        log.info("Checking stock for item: {} at branch: {}", itemId, branchId);

        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stock")
                            .queryParam("itemId", itemId)
                            .queryParam("branchId", branchId)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            Mono.error(new ServiceException("Stock not found for item: " + itemId)))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            Mono.error(new ServiceException("Inventory service unavailable")))
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                return StockCheckResult.builder()
                        .itemId((String) response.get("itemId"))
                        .branchId((String) response.get("branchId"))
                        .availableQuantity((Integer) response.get("availableQuantity"))
                        .unitPrice(response.get("unitPrice") != null ? new BigDecimal(response.get("unitPrice").toString()) : null)
                        .status((String) response.get("status"))
                        .sufficient(true)
                        .build();
            }
            throw new ServiceException("Empty response from inventory service");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking stock: {}", e.getMessage());
            throw new ServiceException("Failed to check stock: " + e.getMessage());
        }
    }

    public ProductionOrderResult createProductionOrder(String itemId, String branchId, int quantity, String reason) {
        log.info("Creating production order for item: {} at branch: {} quantity: {}", itemId, branchId, quantity);

        try {
            Map<String, Object> request = Map.of(
                    "itemId", itemId,
                    "branchId", branchId,
                    "requestedQuantity", quantity,
                    "reason", reason
            );

            Map<String, Object> response = webClient.post()
                    .uri("/production-orders")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            Mono.error(new ServiceException("Failed to create production order")))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            Mono.error(new ServiceException("Inventory service unavailable")))
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                return ProductionOrderResult.builder()
                        .productionOrderId((String) response.get("productionOrderId"))
                        .status((String) response.get("status"))
                        .build();
            }
            throw new ServiceException("Empty response from inventory service");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating production order: {}", e.getMessage());
            throw new ServiceException("Failed to create production order: " + e.getMessage());
        }
    }

}
