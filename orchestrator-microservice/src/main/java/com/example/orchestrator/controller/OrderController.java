package com.example.orchestrator.controller;

import com.example.orchestrator.dto.OrderRequest;
import com.example.orchestrator.dto.OrderResponse;
import com.example.orchestrator.service.OrderOrchestrationService;
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
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Order Orchestration", description = "Order processing orchestration endpoints")
public class OrderController {

    private final OrderOrchestrationService orchestrationService;

    @PostMapping
    @Operation(summary = "Process a new order",
            description = "Orchestrates the complete order flow: stock check, production order (if needed), " +
                    "shipping quote, payment, and shipping confirmation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order processed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content),
            @ApiResponse(responseCode = "422", description = "Orchestration failed at a step",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "Downstream service unavailable",
                    content = @Content)
    })
    public ResponseEntity<OrderResponse> processOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orchestrationService.processOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
