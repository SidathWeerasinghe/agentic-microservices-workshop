package com.example.inventory.controller;

import com.example.inventory.dto.ItemCatalogResponse;
import com.example.inventory.dto.StockResponse;
import com.example.inventory.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Stock management endpoints")
public class StockController {

    private final StockService stockService;

    @GetMapping
    @Operation(summary = "Check stock for an item at a branch",
            description = "Returns the stock details including available quantity and status for a specific item at a branch")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock details retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StockResponse.class))),
            @ApiResponse(responseCode = "404", description = "Item or branch not found",
                    content = @Content)
    })
    public ResponseEntity<StockResponse> checkStock(
            @Parameter(description = "ID of the item to check", required = true)
            @RequestParam String itemId,
            @Parameter(description = "ID of the branch to check", required = true)
            @RequestParam String branchId) {
        
        StockResponse response = stockService.checkStock(itemId, branchId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/catalog")
    @Operation(summary = "Get all items catalog",
            description = "Returns a list of all items with their stock information across all branches. " +
                    "Useful for finding item codes by product name using natural language search.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Items catalog retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ItemCatalogResponse.class))))
    })
    public ResponseEntity<List<ItemCatalogResponse>> getAllItemsCatalog() {
        List<ItemCatalogResponse> catalog = stockService.getAllItemsCatalog();
        return ResponseEntity.ok(catalog);
    }

    @GetMapping("/catalog/search")
    @Operation(summary = "Search items by name or ID",
            description = "Search for items using natural language. Matches item ID or item name (case-insensitive). " +
                    "Example: Search 'macbook' to find all MacBook products, or 'galaxy' to find Samsung Galaxy products.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ItemCatalogResponse.class))))
    })
    public ResponseEntity<List<ItemCatalogResponse>> searchItems(
            @Parameter(description = "Search term (matches item ID or name, case-insensitive)", 
                    required = true,
                    example = "macbook")
            @RequestParam String q) {
        List<ItemCatalogResponse> results = stockService.searchItems(q);
        return ResponseEntity.ok(results);
    }

}
