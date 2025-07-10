package com.winnguyen1905.shipping.core.model.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShipmentRequest {

    @NotNull(message = "Order ID is required")
    @Positive(message = "Order ID must be positive")
    private Long orderId;

    @NotNull(message = "Carrier ID is required")
    @Positive(message = "Carrier ID must be positive")
    private Integer carrierId;

    @NotNull(message = "Shipping method ID is required")
    @Positive(message = "Shipping method ID must be positive")
    private Integer methodId;

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

    @DecimalMin(value = "0.0", message = "Insurance cost cannot be negative")
    private BigDecimal insuranceCost;

    private LocalDate estimatedDeliveryDate;

    private String deliveryNotes;

    @NotEmpty(message = "Shipment items are required")
    private List<ShipmentItemRequest> shipmentItems;

    private List<ShipmentPackageRequest> shipmentPackages;

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

        private String companyName;

        private String contactName;

        private String phoneNumber;

        @Email(message = "Valid email is required")
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentItemRequest {
        @NotNull(message = "Order item ID is required")
        private Long orderItemId;

        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotBlank(message = "Product name is required")
        private String productName;

        private String productSku;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private Integer quantity;

        @NotNull(message = "Unit weight is required")
        @DecimalMin(value = "0.01", message = "Unit weight must be greater than 0")
        private BigDecimal unitWeight;

        private DimensionsRequest dimensions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentPackageRequest {
        @NotBlank(message = "Package number is required")
        private String packageNumber;

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
        private String unit; // CM, INCH
    }
} 
