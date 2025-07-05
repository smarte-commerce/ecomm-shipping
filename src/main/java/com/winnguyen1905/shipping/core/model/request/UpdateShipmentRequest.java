package com.winnguyen1905.shipping.core.model.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.winnguyen1905.shipping.common.enums.ShipmentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShipmentRequest {

    private String trackingNumber;

    private String shippingLabelUrl;

    private CreateShipmentRequest.AddressRequest toAddress;

    @DecimalMin(value = "0.0", message = "Insurance cost cannot be negative")
    private BigDecimal insuranceCost;

    private ShipmentStatus status;

    private LocalDate estimatedDeliveryDate;

    private LocalDate actualDeliveryDate;

    private String deliverySignature;

    private String deliveryNotes;
} 
