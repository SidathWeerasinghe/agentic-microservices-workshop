package com.example.inventory.controller;

import com.example.inventory.dto.ProductionOrderRequest;
import com.example.inventory.dto.ProductionOrderResponse;
import com.example.inventory.service.ProductionOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/production-orders")
@RequiredArgsConstructor
@Tag(name = "Production Orders", description = "Production order management endpoints")
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;

    @PostMapping
    @Operation(summary = "Order production for a branch",
            description = "Creates a new production order for a specific item at a branch. " +
                    "The request is rejected if the requested quantity exceeds the available stock.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Production order created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductionOrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Item or branch not found",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Requested quantity exceeds available stock",
                    content = @Content)
    })
    public ResponseEntity<ProductionOrderResponse> createProductionOrder(
            @Valid @RequestBody ProductionOrderRequest request) {
        
        ProductionOrderResponse response = productionOrderService.createProductionOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
