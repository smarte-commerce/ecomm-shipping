package com.winnguyen1905.shipping.core.model.response;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {

    @JsonProperty("webhook_id")
    private Long webhookId;

    @JsonProperty("shipment_id")
    private Long shipmentId;

    @JsonProperty("carrier_id")
    private Integer carrierId;

    @JsonProperty("carrier_name")
    private String carrierName;

    @JsonProperty("carrier_code")
    private String carrierCode;

    @JsonProperty("webhook_type")
    private String webhookType;

    @JsonProperty("tracking_number")
    private String trackingNumber;

    @JsonProperty("webhook_data")
    private String webhookData;

    @JsonProperty("processed")
    private Boolean processed;

    @JsonProperty("received_at")
    private Instant receivedAt;

    @JsonProperty("processed_at")
    private Instant processedAt;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("retry_count")
    private Integer retryCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookStatistics {
        @JsonProperty("total_webhooks")
        private Long totalWebhooks;

        @JsonProperty("processed_webhooks")
        private Long processedWebhooks;

        @JsonProperty("pending_webhooks")
        private Long pendingWebhooks;

        @JsonProperty("failed_webhooks")
        private Long failedWebhooks;

        @JsonProperty("webhook_types")
        private List<String> webhookTypes;

        @JsonProperty("processing_rate")
        private Double processingRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchProcessResponse {
        @JsonProperty("total_processed")
        private Integer totalProcessed;

        @JsonProperty("successful_processed")
        private Integer successfulProcessed;

        @JsonProperty("failed_processed")
        private Integer failedProcessed;

        @JsonProperty("processing_results")
        private List<ProcessingResult> processingResults;

        @JsonProperty("processed_at")
        private Instant processedAt;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProcessingResult {
            @JsonProperty("webhook_id")
            private Long webhookId;

            @JsonProperty("is_successful")
            private Boolean isSuccessful;

            @JsonProperty("error_message")
            private String errorMessage;

            @JsonProperty("processing_time_ms")
            private Long processingTimeMs;
        }
    }
} 
