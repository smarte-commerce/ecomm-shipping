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
public class CarrierResponse {

    private Integer carrierId;

    private String carrierName;

    private String carrierCode;

    private Boolean isActive;

    private List<String> supportedCountries;

    private String apiEndpoint;

    private CarrierConfiguration configuration;

    private Instant createdAt;

    private Instant updatedAt;

    private List<ShippingMethodResponse> shippingMethods;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CarrierConfiguration {
        private String webhookUrl;

        private Boolean testMode;

        private Integer timeoutSeconds;

        private Integer retryAttempts;

        // API keys are not returned in responses for security reasons
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingMethodResponse {
        private Integer methodId;

        private String methodName;

        private String methodCode;

        private ServiceType serviceType;

        private BigDecimal baseRate;

        private BigDecimal perKgRate;

        private BigDecimal perItemRate;

        private Integer estimatedDaysMin;

        private Integer estimatedDaysMax;

        private Boolean isActive;

        private String zoneName;

        private String zoneCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionTestResponse {
        private Boolean isSuccessful;

        private Long responseTimeMs;

        private String testMessage;

        private String errorMessage;

        private Instant testedAt;
    }
} 
