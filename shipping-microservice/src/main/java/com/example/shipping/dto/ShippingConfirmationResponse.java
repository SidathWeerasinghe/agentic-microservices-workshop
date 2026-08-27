package com.example.shipping.dto;

import com.example.shipping.enums.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingConfirmationResponse {

    private String shipmentId;
    private ShipmentStatus status;

}
