package com.winnguyen1905.shipping.core.model.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ReportsResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingOverview {
        private Long totalShipments;
        private Long totalPackages;
        private BigDecimal totalRevenue;
        private Double averageDeliveryDays;
        private Double successRate;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private Double growthRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CarrierPerformance {
        private Integer carrierId;
        private String carrierName;
        private Long shipmentCount;
        private Double onTimeDeliveryRate;
        private Double averageDeliveryDays;
        private BigDecimal totalRevenue;
        private BigDecimal costPerShipment;
        private Double customerSatisfaction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneAnalytics {
        private Integer zoneId;
        private String zoneName;
        private Long shipmentCount;
        private BigDecimal averageShippingCost;
        private String mostPopularServiceType;
        private Double deliverySuccessRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueAnalytics {
        private String period;
        private BigDecimal totalRevenue;
        private BigDecimal shippingRevenue;
        private BigDecimal insuranceRevenue;
        private Map<String, BigDecimal> revenueByCarrier;
        private Map<String, BigDecimal> revenueByServiceType;
        private List<MonthlyRevenue> monthlyBreakdown;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MonthlyRevenue {
            private String month;
            private BigDecimal revenue;
            private Long shipmentCount;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationalMetrics {
        private Long totalActiveShipments;
        private Long pendingShipments;
        private Long inTransitShipments;
        private Long deliveredToday;
        private Long failedDeliveries;
        private Double averageProcessingTime;
        private List<Integer> peakShippingHours;
        private Double webhookProcessingRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInsights {
        private List<Destination> topDestinations;
        private Map<String, Long> preferredServiceTypes;
        private BigDecimal averagePackageWeight;
        private Double repeatCustomerRate;
        private BigDecimal customerAcquisitionCost;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Destination {
            private String country;
            private String city;
            private Long shipmentCount;
            private Double percentage;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendAnalysis {
        private String metricName;
        private String trendDirection; // UP, DOWN, STABLE
        private Double percentageChange;
        private List<DataPoint> dataPoints;
        private List<DataPoint> forecast;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DataPoint {
            private LocalDate date;
            private BigDecimal value;
        }
    }
} 
