package com.winnguyen1905.shipping.core.model.request;

import java.math.BigDecimal;
import java.util.List;

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
public class CalculateRateRequest {

    @JsonProperty("order_id")
    private Long orderId;

    @NotNull(message = "From address is required")
    @JsonProperty("from_address")
    private AddressRequest fromAddress;

    @NotNull(message = "To address is required")
    @JsonProperty("to_address")
    private AddressRequest toAddress;

    @NotNull(message = "Total weight is required")
    @DecimalMin(value = "0.01", message = "Total weight must be greater than 0")
    @JsonProperty("total_weight")
    private BigDecimal totalWeight;

    @NotNull(message = "Total value is required")
    @DecimalMin(value = "0.01", message = "Total value must be greater than 0")
    @JsonProperty("total_value")
    private BigDecimal totalValue;

    @Positive(message = "Package count must be positive")
    @JsonProperty("package_count")
    private Integer packageCount;

    @JsonProperty("requested_service_type")
    private ServiceType requestedServiceType;

    @JsonProperty("specific_carrier_id")
    private Integer specificCarrierId;

    @JsonProperty("specific_method_id")
    private Integer specificMethodId;

    @JsonProperty("packages")
    private List<PackageRequest> packages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressRequest {
        @NotBlank(message = "Address line 1 is required")
        @JsonProperty("address_line_1")
        private String addressLine1;

        @JsonProperty("address_line_2")
        private String addressLine2;

        @NotBlank(message = "City is required")
        private String city;

        @NotBlank(message = "State is required")
        private String state;

        @NotBlank(message = "Postal code is required")
        @JsonProperty("postal_code")
        private String postalCode;

        @NotBlank(message = "Country is required")
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageRequest {
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
    }

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
        private String unit;
    }
} 
