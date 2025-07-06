package com.winnguyen1905.shipping.core.model.request;

import java.math.BigDecimal;
import java.util.List;

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
public class CalculateRateRequest {

    private Long orderId;

    @NotNull(message = "From address is required")
    private AddressRequest fromAddress;

    @NotNull(message = "To address is required")
    private AddressRequest toAddress;

    @NotNull(message = "Total weight is required")
    @DecimalMin(value = "0.01", message = "Total weight must be greater than 0")
    private BigDecimal totalWeight;

    @NotNull(message = "Total value is required")
    @DecimalMin(value = "0.01", message = "Total value must be greater than 0")
    private BigDecimal totalValue;

    @Positive(message = "Package count must be positive")
    private Integer packageCount;

    private ServiceType requestedServiceType;

    private Integer specificCarrierId;

    private Integer specificMethodId;

    private List<PackageRequest> packages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressRequest {
        @NotBlank(message = "Address line 1 is required")
        private String addressLine1;

        private String addressLine2;

        @NotBlank(message = "City is required")
        private String city;

        @NotBlank(message = "State is required")
        private String state;

        @NotBlank(message = "Postal code is required")
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

        private String packageType;

        private Boolean isFragile;

        private Boolean isLiquid;

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
