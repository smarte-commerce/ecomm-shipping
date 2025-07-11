package com.winnguyen1905.shipping.core.model.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.winnguyen1905.shipping.common.enums.ShipmentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {

    private Long shipmentId;
    private Long orderId;
    private String shipmentNumber;
    private String trackingNumber;
    private String shippingLabelUrl;
    private AddressResponse fromAddress;
    private AddressResponse toAddress;
    private Integer packageCount;
    private BigDecimal totalWeight;
    private BigDecimal totalValue;
    private BigDecimal shippingCost;
    private BigDecimal insuranceCost;
    private ShipmentStatus status;
    private Instant shippedAt;
    private LocalDate estimatedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private String deliverySignature;
    private String deliveryNotes;
    private Instant createdAt;
    private Instant updatedAt;
    private CarrierResponse carrier;
    private ShippingMethodResponse shippingMethod;
    private List<ShipmentItemResponse> shipmentItems;
    private List<ShipmentPackageResponse> shipmentPackages;
    private List<TrackingEventResponse> trackingEvents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressResponse {
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String companyName;
        private String contactName;
        private String phoneNumber;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CarrierResponse {
        private Integer carrierId;
        private String carrierName;
        private String carrierCode;
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingMethodResponse {
        private Integer methodId;
        private String methodName;
        private String methodCode;
        private String serviceType;
        private BigDecimal baseRate;
        private Integer estimatedDaysMin;
        private Integer estimatedDaysMax;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentItemResponse {
        private Long shipmentItemId;
        private Long orderItemId;
        private Long productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitWeight;
        private BigDecimal totalWeight;
        private DimensionsResponse dimensions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentPackageResponse {
        private Long packageId;
        private String packageNumber;
        private String trackingNumber;
        private BigDecimal weight;
        private DimensionsResponse dimensions;
        private String packageType;
        private Boolean isFragile;
        private Boolean isLiquid;
        private Boolean isHazardous;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingEventResponse {
        private Long eventId;
        private String trackingNumber;
        private String eventType;
        private String eventDescription;
        private String eventLocation;
        private Instant eventTimestamp;
        private String carrierEventCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionsResponse {
        private BigDecimal length;
        private BigDecimal width;
        private BigDecimal height;
        private String unit;
    }
} 
