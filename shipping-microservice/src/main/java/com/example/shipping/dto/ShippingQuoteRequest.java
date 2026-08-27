package com.example.shipping.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingQuoteRequest {

    @NotBlank(message = "Branch ID is required")
    private String branchId;

    @NotNull(message = "Destination address is required")
    @Valid
    private Address destinationAddress;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<ShippingItem> items;

}
