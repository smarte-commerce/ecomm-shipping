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
public class WebhookResponse {

    private Long webhookId;

    private Long shipmentId;

    private Integer carrierId;

    private String carrierName;

    private String carrierCode;

    private String webhookType;

    private String trackingNumber;

    private String webhookData;

    private Boolean processed;

    private Instant receivedAt;

    private Instant processedAt;

    private String errorMessage;

    private Integer retryCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookStatistics {
        private Long totalWebhooks;

        private Long processedWebhooks;

        private Long pendingWebhooks;

        private Long failedWebhooks;

        private List<String> webhookTypes;

        private Double processingRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchProcessResponse {
        private Integer totalProcessed;

        private Integer successfulProcessed;

        private Integer failedProcessed;

        private List<ProcessingResult> processingResults;

        private Instant processedAt;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProcessingResult {
            private Long webhookId;

            private Boolean isSuccessful;

            private String errorMessage;

            private Long processingTimeMs;
        }
    }
} 
