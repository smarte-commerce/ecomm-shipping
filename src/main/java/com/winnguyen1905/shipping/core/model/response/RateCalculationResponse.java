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
public class RateCalculationResponse {

    private Long calculationId;

    private Long orderId;

    private String fromZip;

    private String toZip;

    private BigDecimal totalWeight;

    private BigDecimal totalValue;

    private Integer packageCount;

    private ServiceType requestedServiceType;

    private Instant calculatedAt;

    private List<ShippingRate> availableRates;

    private ShippingRate cheapestRate;

    private ShippingRate fastestRate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingRate {
        private Integer methodId;

        private Integer carrierId;

        private String carrierName;

        private String carrierCode;

        private String methodName;

        private String methodCode;

        private ServiceType serviceType;

        private BigDecimal baseRate;

        private BigDecimal weightRate;

        private BigDecimal totalRate;

        private String currency;

        private Integer estimatedDaysMin;

        private Integer estimatedDaysMax;

        private String estimatedDeliveryDate;

        private Boolean isAvailable;

        private String unavailableReason;

        private String zoneName;

        private String zoneCode;
    }
} 
