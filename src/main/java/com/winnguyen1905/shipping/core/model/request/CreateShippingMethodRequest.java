package com.winnguyen1905.shipping.core.model.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.winnguyen1905.shipping.common.enums.ServiceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShippingMethodRequest {

    @NotNull(message = "Carrier ID is required")
    private Integer carrierId;

    @NotNull(message = "Zone ID is required")
    private Integer zoneId;

    @NotBlank(message = "Method name is required")
    @Size(max = 100, message = "Method name must not exceed 100 characters")
    private String methodName;

    @NotBlank(message = "Method code is required")
    @Size(max = 50, message = "Method code must not exceed 50 characters")
    private String methodCode;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotNull(message = "Base rate is required")
    @DecimalMin(value = "0.0", message = "Base rate cannot be negative")
    private BigDecimal baseRate;

    @NotNull(message = "Per kg rate is required")
    @DecimalMin(value = "0.0", message = "Per kg rate cannot be negative")
    private BigDecimal perKgRate;

    @NotNull(message = "Per item rate is required")
    @DecimalMin(value = "0.0", message = "Per item rate cannot be negative")
    private BigDecimal perItemRate;

    private BigDecimal minWeight;

    private BigDecimal maxWeight;

    private BigDecimal minOrderValue;

    private BigDecimal maxOrderValue;

    private Integer estimatedDaysMin;

    private Integer estimatedDaysMax;

    private Boolean isActive;
} 
