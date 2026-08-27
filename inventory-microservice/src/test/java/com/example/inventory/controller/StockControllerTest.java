package com.example.inventory.controller;

import com.example.inventory.dto.StockResponse;
import com.example.inventory.enums.StockStatus;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockController.class)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockService stockService;

    @Test
    void checkStock_WhenStockExists_ReturnsStockResponse() throws Exception {
        StockResponse response = StockResponse.builder()
                .itemId("ITEM-001")
                .branchId("BRANCH-NYC")
                .availableQuantity(100)
                .unitPrice(new BigDecimal("100.0"))
                .status(StockStatus.IN_STOCK)
                .build();

        when(stockService.checkStock("ITEM-001", "BRANCH-NYC")).thenReturn(response);

        mockMvc.perform(get("/stock")
                        .param("itemId", "ITEM-001")
                        .param("branchId", "BRANCH-NYC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value("ITEM-001"))
                .andExpect(jsonPath("$.branchId").value("BRANCH-NYC"))
                .andExpect(jsonPath("$.availableQuantity").value(100))
                .andExpect(jsonPath("$.unitPrice").value(100.0))
                .andExpect(jsonPath("$.status").value("IN_STOCK"));
    }

    @Test
    void checkStock_WhenStockNotFound_Returns404() throws Exception {
        when(stockService.checkStock("ITEM-999", "BRANCH-NYC"))
                .thenThrow(new ResourceNotFoundException("Stock not found"));

        mockMvc.perform(get("/stock")
                        .param("itemId", "ITEM-999")
                        .param("branchId", "BRANCH-NYC"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkStock_WhenMissingParameters_Returns400() throws Exception {
        mockMvc.perform(get("/stock")
                        .param("itemId", "ITEM-001"))
                .andExpect(status().isBadRequest());
    }

}
