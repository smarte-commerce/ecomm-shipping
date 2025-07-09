package com.winnguyen1905.shipping.core.model.response;

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
public class TrackingEventResponse {

    private Long eventId;

    private Long shipmentId;

    private String trackingNumber;

    private String eventType;

    private String eventDescription;

    private String eventLocation;

    private Instant eventTimestamp;

    private String carrierEventCode;

    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingStatus {
        private String trackingNumber;

        private String currentStatus;

        private String currentLocation;

        private Instant lastUpdated;

        private String deliveryStatus;

        private String estimatedDelivery;

        private Integer totalEvents;

        private String carrierName;

        private String serviceType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchRefreshResponse {
        private Integer totalRequested;

        private Integer successfulRefreshes;

        private Integer failedRefreshes;

        private List<RefreshResult> refreshResults;

        private Instant processedAt;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RefreshResult {
            private String trackingNumber;

            private Boolean isSuccessful;

            private Integer newEventsCount;

            private String errorMessage;
        }
    }
} 
