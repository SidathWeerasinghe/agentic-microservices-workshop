package com.example.shipping.controller;

import com.example.shipping.dto.*;
import com.example.shipping.enums.ShipmentStatus;
import com.example.shipping.exception.QuoteNotFoundException;
import com.example.shipping.service.ShippingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShippingController.class)
class ShippingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShippingService shippingService;

    @Test
    void calculateShippingQuote_WithValidRequest_Returns200() throws Exception {
        ShippingQuoteRequest request = ShippingQuoteRequest.builder()
                .branchId("BRANCH-NYC")
                .destinationAddress(Address.builder()
                        .line1("123 Main St")
                        .city("Colombo")
                        .area("Colombo 07")
                        .country("Sri Lanka")
                        .build())
                .items(List.of(ShippingItem.builder()
                        .itemId("ITEM-001")
                        .quantity(2)
                        .weightKg(new BigDecimal("1.5"))
                        .build()))
                .build();

        ShippingQuoteResponse response = ShippingQuoteResponse.builder()
                .quoteId("QUOTE-12345")
                .shippingCost(new BigDecimal("250.00"))
                .currency("LKR")
                .build();

        when(shippingService.calculateShippingQuote(any(ShippingQuoteRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId").value("QUOTE-12345"))
                .andExpect(jsonPath("$.shippingCost").value(250.00))
                .andExpect(jsonPath("$.currency").value("LKR"));
    }

    @Test
    void confirmShipping_WithValidRequest_Returns201() throws Exception {
        ShippingConfirmationRequest request = ShippingConfirmationRequest.builder()
                .quoteId("QUOTE-12345")
                .orderId("ORDER-001")
                .customerId("CUST-001")
                .build();

        ShippingConfirmationResponse response = ShippingConfirmationResponse.builder()
                .shipmentId("SHIP-12345")
                .status(ShipmentStatus.CONFIRMED)
                .build();

        when(shippingService.confirmShipping(any(ShippingConfirmationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shipmentId").value("SHIP-12345"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void confirmShipping_WithInvalidQuote_Returns404() throws Exception {
        ShippingConfirmationRequest request = ShippingConfirmationRequest.builder()
                .quoteId("INVALID-QUOTE")
                .orderId("ORDER-001")
                .customerId("CUST-001")
                .build();

        when(shippingService.confirmShipping(any(ShippingConfirmationRequest.class)))
                .thenThrow(new QuoteNotFoundException("Quote not found"));

        mockMvc.perform(post("/confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

}
