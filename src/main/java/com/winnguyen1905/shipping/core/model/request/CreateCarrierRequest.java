package com.winnguyen1905.shipping.core.model.request;

import java.util.List;

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
public class CreateCarrierRequest {

    @NotBlank(message = "Carrier name is required")
    @Size(max = 100, message = "Carrier name must not exceed 100 characters")
    @JsonProperty("carrier_name")
    private String carrierName;

    @NotBlank(message = "Carrier code is required")
    @Size(max = 20, message = "Carrier code must not exceed 20 characters")
    @JsonProperty("carrier_code")
    private String carrierCode;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("supported_countries")
    private List<String> supportedCountries;

    @JsonProperty("api_endpoint")
    private String apiEndpoint;

    @JsonProperty("configuration")
    private CarrierConfiguration configuration;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CarrierConfiguration {
        @JsonProperty("api_key")
        private String apiKey;

        @JsonProperty("api_secret")
        private String apiSecret;

        @JsonProperty("webhook_url")
        private String webhookUrl;

        @JsonProperty("test_mode")
        private Boolean testMode;

        @JsonProperty("timeout_seconds")
        private Integer timeoutSeconds;

        @JsonProperty("retry_attempts")
        private Integer retryAttempts;
    }
} 
