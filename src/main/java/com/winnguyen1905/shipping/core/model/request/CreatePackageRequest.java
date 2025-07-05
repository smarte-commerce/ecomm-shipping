package com.winnguyen1905.shipping.core.model.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePackageRequest {

    @NotNull(message = "Shipment ID is required")
    @JsonProperty("shipment_id")
    private Long shipmentId;

    @NotBlank(message = "Package number is required")
    @Size(max = 50, message = "Package number must not exceed 50 characters")
    @JsonProperty("package_number")
    private String packageNumber;

    @JsonProperty("tracking_number")
    private String trackingNumber;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.01", message = "Weight must be greater than 0")
    private BigDecimal weight;

    @NotNull(message = "Dimensions are required")
    private DimensionsRequest dimensions;

    @JsonProperty("package_type")
    private String packageType;

    @JsonProperty("is_fragile")
    private Boolean isFragile;

    @JsonProperty("is_liquid")
    private Boolean isLiquid;

    @JsonProperty("is_hazardous")
    private Boolean isHazardous;

    @JsonProperty("special_instructions")
    private String specialInstructions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionsRequest {
        @NotNull(message = "Length is required")
        @DecimalMin(value = "0.01", message = "Length must be greater than 0")
        private BigDecimal length;

        @NotNull(message = "Width is required")
        @DecimalMin(value = "0.01", message = "Width must be greater than 0")
        private BigDecimal width;

        @NotNull(message = "Height is required")
        @DecimalMin(value = "0.01", message = "Height must be greater than 0")
        private BigDecimal height;

        @NotBlank(message = "Unit is required")
        private String unit; // CM, INCH
    }
} 
