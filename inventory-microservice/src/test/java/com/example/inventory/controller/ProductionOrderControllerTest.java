package com.example.inventory.controller;

import com.example.inventory.dto.ProductionOrderRequest;
import com.example.inventory.dto.ProductionOrderResponse;
import com.example.inventory.enums.OrderStatus;
import com.example.inventory.service.ProductionOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductionOrderController.class)
class ProductionOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductionOrderService productionOrderService;

    @Test
    void createProductionOrder_WithValidRequest_Returns201() throws Exception {
        ProductionOrderRequest request = ProductionOrderRequest.builder()
                .itemId("ITEM-001")
                .branchId("BRANCH-NYC")
                .requestedQuantity(50)
                .reason("Low stock replenishment")
                .build();

        ProductionOrderResponse response = ProductionOrderResponse.builder()
                .productionOrderId("PO-12345")
                .status(OrderStatus.CREATED)
                .build();

        when(productionOrderService.createProductionOrder(any(ProductionOrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/production-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productionOrderId").value("PO-12345"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createProductionOrder_WithMissingItemId_Returns400() throws Exception {
        ProductionOrderRequest request = ProductionOrderRequest.builder()
                .branchId("BRANCH-NYC")
                .requestedQuantity(50)
                .build();

        mockMvc.perform(post("/production-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProductionOrder_WithNegativeQuantity_Returns400() throws Exception {
        ProductionOrderRequest request = ProductionOrderRequest.builder()
                .itemId("ITEM-001")
                .branchId("BRANCH-NYC")
                .requestedQuantity(-10)
                .build();

        mockMvc.perform(post("/production-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}
