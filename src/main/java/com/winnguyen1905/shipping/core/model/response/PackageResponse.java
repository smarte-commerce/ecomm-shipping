package com.winnguyen1905.shipping.core.model.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageResponse {

    private Long packageId;

    private Long shipmentId;

    private String shipmentNumber;

    private String packageNumber;

    private String trackingNumber;

    private BigDecimal weight;

    private DimensionsResponse dimensions;

    private String packageType;

    private Boolean isFragile;

    private Boolean isLiquid;

    private Boolean isHazardous;

    private String specialInstructions;

    private Instant createdAt;

    private String carrierName;

    private String currentStatus;

    private String currentLocation;

    private Instant lastUpdate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionsResponse {
        private BigDecimal length;
        
        private BigDecimal width;
        
        private BigDecimal height;
        
        private String unit;

        private BigDecimal volume;

        private BigDecimal dimensionalWeight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageTrackingInfo {
        private Long packageId;

        private String packageNumber;

        private String trackingNumber;

        private String currentStatus;

        private String currentLocation;

        private Instant lastScanTime;

        private String estimatedDelivery;

        private List<TrackingEvent> trackingEvents;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TrackingEvent {
            private String eventType;

            private String eventDescription;

            private String eventLocation;

            private Instant eventTimestamp;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageValidationResult {
        private Long packageId;

        private Boolean isValid;

        private List<String> validationErrors;

        private List<String> validationWarnings;

        private List<String> complianceIssues;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkPackageOperation {
        private Integer totalPackages;

        private Integer successfulOperations;

        private Integer failedOperations;

        private List<OperationResult> operationResults;

        private Instant processedAt;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OperationResult {
            private Long packageId;

            private String packageNumber;

            private Boolean isSuccessful;

            private String errorMessage;
        }
    }
} 
