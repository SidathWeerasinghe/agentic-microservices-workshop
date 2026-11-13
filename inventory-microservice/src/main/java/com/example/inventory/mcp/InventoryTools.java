package com.example.inventory.mcp;

import com.example.inventory.dto.ItemCatalogResponse;
import com.example.inventory.dto.ProductionOrderRequest;
import com.example.inventory.dto.ProductionOrderResponse;
import com.example.inventory.dto.StockResponse;
import com.example.inventory.service.ProductionOrderService;
import com.example.inventory.service.StockService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP tool adapters for the inventory domain.
 * <p>
 * These are thin wrappers over the existing {@link StockService} and
 * {@link ProductionOrderService} beans - the same business logic the REST
 * controllers use. No logic is duplicated here; this class only exposes the
 * service layer as MCP tools. The REST controllers remain untouched.
 * <p>
 * Tool arguments come from an LLM, so they are treated as untrusted input: the same
 * Bean Validation rules the REST layer enforces with {@code @Valid} are applied here
 * before the service is called, and a clear message is returned to the agent.
 */
@Component
@RequiredArgsConstructor
public class InventoryTools {

    private final StockService stockService;
    private final ProductionOrderService productionOrderService;
    private final Validator validator;

    @Tool(description = "Check stock availability and status for one item at one branch. "
            + "Returns availableQuantity, unitPrice (LKR) and status (IN_STOCK, LOW_STOCK or OUT_OF_STOCK). "
            + "Use item and branch IDs returned by searchItems; do not guess them.")
    public StockResponse checkStock(
            @ToolParam(description = "Item ID, e.g. MACBOOK-PRO-001") String itemId,
            @ToolParam(description = "Branch ID, e.g. BRANCH-COL") String branchId) {
        requireText(itemId, "itemId");
        requireText(branchId, "branchId");
        return stockService.checkStock(itemId, branchId);
    }

    @Tool(description = "Search the item catalog by product name or item ID (case-insensitive). "
            + "Use this to resolve a natural-language product name into an item ID. "
            + "Each result lists every branch that stocks the item with its quantity and unit price (LKR). "
            + "Example: 'macbook' finds all MacBook products, 'galaxy' finds Samsung Galaxy products.")
    public List<ItemCatalogResponse> searchItems(
            @ToolParam(description = "Search term matching item ID or name") String q) {
        requireText(q, "q");
        return stockService.searchItems(q);
    }

    @Tool(description = "List the entire item catalog with stock information across all branches. "
            + "Prefer searchItems when you already know part of the product name.")
    public List<ItemCatalogResponse> getAllItemsCatalog() {
        return stockService.getAllItemsCatalog();
    }

    @Tool(description = "Create a production order that allocates stock for a customer purchase: "
            + "it DEDUCTS requestedQuantity from the item's available stock at the branch. "
            + "Rejected if requestedQuantity exceeds available stock. Write operation - call it once per "
            + "purchased item, after checking stock and before taking payment. It does not add stock.")
    public ProductionOrderResponse createProductionOrder(
            @ToolParam(description = "Production order details: itemId, branchId, requestedQuantity (positive), "
                    + "and optional reason (e.g. the order ID)")
            ProductionOrderRequest request) {
        return productionOrderService.createProductionOrder(validate(request));
    }

    private <T> T validate(T request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid request: " + violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
        return request;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
