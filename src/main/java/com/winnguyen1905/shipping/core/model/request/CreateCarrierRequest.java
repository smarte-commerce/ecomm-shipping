package com.winnguyen1905.shipping.core.model.request;

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
public class CreateCarrierRequest {

    @NotBlank(message = "Carrier name is required")
    @Size(max = 100, message = "Carrier name must not exceed 100 characters")
    private String carrierName;

    @NotBlank(message = "Carrier code is required")
    @Size(max = 20, message = "Carrier code must not exceed 20 characters")
    private String carrierCode;

    private Boolean isActive;

    private List<String> supportedCountries;

    private String apiEndpoint;

    private CarrierConfiguration configuration;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CarrierConfiguration {
        private String apiKey;

        private String apiSecret;

        private String webhookUrl;

        private Boolean testMode;

        private Integer timeoutSeconds;

        private Integer retryAttempts;
    }
} 
