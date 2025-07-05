package com.winnguyen1905.shipping.core.model.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.winnguyen1905.shipping.common.enums.ServiceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShippingMethodRequest {

    @NotNull(message = "Carrier ID is required")
    @JsonProperty("carrier_id")
    private Integer carrierId;

    @NotNull(message = "Zone ID is required")
    @JsonProperty("zone_id")
    private Integer zoneId;

    @NotBlank(message = "Method name is required")
    @Size(max = 100, message = "Method name must not exceed 100 characters")
    @JsonProperty("method_name")
    private String methodName;

    @NotBlank(message = "Method code is required")
    @Size(max = 50, message = "Method code must not exceed 50 characters")
    @JsonProperty("method_code")
    private String methodCode;

    @NotNull(message = "Service type is required")
    @JsonProperty("service_type")
    private ServiceType serviceType;

    @NotNull(message = "Base rate is required")
    @DecimalMin(value = "0.0", message = "Base rate cannot be negative")
    @JsonProperty("base_rate")
    private BigDecimal baseRate;

    @NotNull(message = "Per kg rate is required")
    @DecimalMin(value = "0.0", message = "Per kg rate cannot be negative")
    @JsonProperty("per_kg_rate")
    private BigDecimal perKgRate;

    @NotNull(message = "Per item rate is required")
    @DecimalMin(value = "0.0", message = "Per item rate cannot be negative")
    @JsonProperty("per_item_rate")
    private BigDecimal perItemRate;

    @JsonProperty("min_weight")
    private BigDecimal minWeight;

    @JsonProperty("max_weight")
    private BigDecimal maxWeight;

    @JsonProperty("min_order_value")
    private BigDecimal minOrderValue;

    @JsonProperty("max_order_value")
    private BigDecimal maxOrderValue;

    @JsonProperty("estimated_days_min")
    private Integer estimatedDaysMin;

    @JsonProperty("estimated_days_max")
    private Integer estimatedDaysMax;

    @JsonProperty("is_active")
    private Boolean isActive;
} 
