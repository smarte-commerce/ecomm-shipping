package com.winnguyen1905.shipping.core.model.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.winnguyen1905.shipping.common.enums.ServiceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingMethodResponse {

    private Integer methodId;

    private Integer carrierId;

    private String carrierName;

    private String carrierCode;

    private Integer zoneId;

    private String zoneName;

    private String zoneCode;

    private String methodName;

    private String methodCode;

    private ServiceType serviceType;

    private BigDecimal baseRate;

    private BigDecimal perKgRate;

    private BigDecimal perItemRate;

    private BigDecimal minWeight;

    private BigDecimal maxWeight;

    private BigDecimal minOrderValue;

    private BigDecimal maxOrderValue;

    private Integer estimatedDaysMin;

    private Integer estimatedDaysMax;

    private Boolean isActive;

    private Instant createdAt;

    private Instant updatedAt;

    private Long shipmentCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingMethodAvailability {
        private Integer methodId;

        private String methodName;

        private String methodCode;

        private ServiceType serviceType;

        private Boolean isAvailable;

        private String unavailableReason;

        private BigDecimal estimatedRate;

        private Integer estimatedDaysMin;

        private Integer estimatedDaysMax;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingMethodStatistics {
        private Integer methodId;

        private String methodName;

        private Long totalShipments;

        private BigDecimal totalRevenue;

        private Double averageDeliveryDays;

        private Double successRate;

        private Instant mostRecentShipment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkUpdateResult {
        private Integer totalRequested;

        private Integer successfulUpdates;

        private Integer failedUpdates;

        private List<UpdateResult> updateResults;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class UpdateResult {
            private Integer methodId;

            private Boolean isSuccessful;

            private String errorMessage;
        }
    }
} 
