package com.example.payment.controller;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.dto.PaymentRetryRequest;
import com.example.payment.enums.PaymentMethod;
import com.example.payment.enums.PaymentStatus;
import com.example.payment.exception.PaymentNotFoundException;
import com.example.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void makePayment_WithValidRequest_Returns201() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .orderId("ORDER-001")
                .amount(new BigDecimal("1500.00"))
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD)
                .customerId("CUST-001")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .paymentId("pay-123")
                .paymentReference("PAY-ABC12345")
                .orderId("ORDER-001")
                .status(PaymentStatus.SUCCESS)
                .amount(new BigDecimal("1500.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.makePayment(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentReference").value("PAY-ABC12345"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void makePayment_WithMissingOrderId_Returns400() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("1500.00"))
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retryPayment_WithValidReference_Returns200() throws Exception {
        PaymentRetryRequest request = PaymentRetryRequest.builder()
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .reason("Card declined, trying bank transfer")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .paymentId("pay-123")
                .paymentReference("PAY-ABC12345")
                .orderId("ORDER-001")
                .status(PaymentStatus.SUCCESS)
                .amount(new BigDecimal("1500.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.retryPayment(eq("PAY-ABC12345"), any(PaymentRetryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/payments/PAY-ABC12345/retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void retryPayment_WithInvalidReference_Returns404() throws Exception {
        when(paymentService.retryPayment(eq("PAY-INVALID"), any()))
                .thenThrow(new PaymentNotFoundException("Payment not found"));

        mockMvc.perform(post("/payments/PAY-INVALID/retry")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayment_WithValidReference_Returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .paymentId("pay-123")
                .paymentReference("PAY-ABC12345")
                .orderId("ORDER-001")
                .status(PaymentStatus.SUCCESS)
                .amount(new BigDecimal("1500.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.getPayment("PAY-ABC12345")).thenReturn(response);

        mockMvc.perform(get("/payments/PAY-ABC12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference").value("PAY-ABC12345"))
                .andExpect(jsonPath("$.orderId").value("ORDER-001"));
    }

}
