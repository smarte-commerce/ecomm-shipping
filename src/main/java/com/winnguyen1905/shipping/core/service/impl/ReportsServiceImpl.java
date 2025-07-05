package com.winnguyen1905.shipping.core.service.impl;

import com.winnguyen1905.shipping.core.model.response.ReportsResponse;
import com.winnguyen1905.shipping.core.service.ReportsService;
import com.winnguyen1905.shipping.persistance.entity.EShipment;
import com.winnguyen1905.shipping.persistance.entity.EShippingCarrier;
import com.winnguyen1905.shipping.persistance.entity.EShippingWebhook;
import com.winnguyen1905.shipping.persistance.entity.EShippingZone;
import com.winnguyen1905.shipping.persistance.repository.ShipmentRepository;
import com.winnguyen1905.shipping.persistance.repository.ShippingCarrierRepository;
import com.winnguyen1905.shipping.persistance.repository.ShippingWebhookRepository;
import com.winnguyen1905.shipping.persistance.repository.ShippingZoneRepository;
import com.winnguyen1905.shipping.common.enums.ShipmentStatus;
import com.winnguyen1905.shipping.secure.TAccountRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportsServiceImpl implements ReportsService {

    private final ShipmentRepository shipmentRepository;
    private final ShippingCarrierRepository carrierRepository;
    private final ShippingZoneRepository zoneRepository;
    private final ShippingWebhookRepository webhookRepository;

    @Override
    public ReportsResponse.ShippingOverview getShippingOverview(LocalDate startDate, LocalDate endDate, TAccountRequest accountRequest) {
        log.info("Generating shipping overview report from {} to {} for account: {}", startDate, endDate, accountRequest.username());
        
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        // Get shipments in date range
        List<EShipment> shipments = shipmentRepository.findByCreatedAtBetween(startInstant, endInstant);
        
        // Calculate basic metrics
        long totalShipments = shipments.size();
        long totalPackages = shipments.stream()
                .mapToLong(s -> s.getPackageCount() != null ? s.getPackageCount() : 1)
                .sum();
        
        BigDecimal totalRevenue = shipments.stream()
                .map(EShipment::getShippingCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate average delivery days
        List<EShipment> deliveredShipments = shipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.DELIVERED && 
                            s.getShippedAt() != null && 
                            s.getActualDeliveryDate() != null)
                .collect(Collectors.toList());
        
        double averageDeliveryDays = deliveredShipments.stream()
                .mapToLong(s -> ChronoUnit.DAYS.between(
                        s.getShippedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                        s.getActualDeliveryDate()))
                .average()
                .orElse(0.0);
        
        // Calculate success rate
        long successfulDeliveries = shipments.stream()
                .mapToLong(s -> s.getStatus() == ShipmentStatus.DELIVERED ? 1 : 0)
                .sum();
        
        double successRate = totalShipments > 0 ? 
                (double) successfulDeliveries / totalShipments * 100 : 0.0;
        
        // Calculate growth rate (compare with previous period)
        LocalDate previousPeriodStart = startDate.minusDays(ChronoUnit.DAYS.between(startDate, endDate));
        LocalDate previousPeriodEnd = startDate.minusDays(1);
        
        Instant prevStartInstant = previousPeriodStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant prevEndInstant = previousPeriodEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        List<EShipment> previousPeriodShipments = shipmentRepository.findByCreatedAtBetween(prevStartInstant, prevEndInstant);
        
        double growthRate = 0.0;
        if (!previousPeriodShipments.isEmpty()) {
            growthRate = ((double) totalShipments - previousPeriodShipments.size()) / 
                        previousPeriodShipments.size() * 100;
        }
        
        return ReportsResponse.ShippingOverview.builder()
                .totalShipments(totalShipments)
                .totalPackages(totalPackages)
                .totalRevenue(totalRevenue)
                .averageDeliveryDays(averageDeliveryDays)
                .successRate(successRate)
                .periodStart(startDate)
                .periodEnd(endDate)
                .growthRate(growthRate)
                .build();
    }

    @Override
    public List<ReportsResponse.CarrierPerformance> getCarrierPerformance(LocalDate startDate, LocalDate endDate, TAccountRequest accountRequest) {
        log.info("Generating carrier performance report from {} to {} for account: {}", startDate, endDate, accountRequest.username());
        
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        List<EShipment> shipments = shipmentRepository.findByCreatedAtBetween(startInstant, endInstant);
        List<EShippingCarrier> carriers = carrierRepository.findAllActiveCarriers();
        
        return carriers.stream()
                .map(carrier -> {
                    List<EShipment> carrierShipments = shipments.stream()
                            .filter(s -> s.getCarrier().getCarrierId().equals(carrier.getCarrierId()))
                            .collect(Collectors.toList());
                    
                    return buildCarrierPerformance(carrier, carrierShipments);
                })
                .filter(performance -> performance.getShipmentCount() > 0)
                .sorted(Comparator.comparing(ReportsResponse.CarrierPerformance::getOnTimeDeliveryRate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportsResponse.ZoneAnalytics> getZoneAnalytics(LocalDate startDate, LocalDate endDate, TAccountRequest accountRequest) {
        log.info("Generating zone analytics report from {} to {} for account: {}", startDate, endDate, accountRequest.username());
        
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        List<EShipment> shipments = shipmentRepository.findByCreatedAtBetween(startInstant, endInstant);
        List<EShippingZone> zones = zoneRepository.findAllActiveZones();
        
        return zones.stream()
                .map(zone -> {
                    List<EShipment> zoneShipments = shipments.stream()
                            .filter(s -> s.getMethod().getZone().getZoneId().equals(zone.getZoneId()))
                            .collect(Collectors.toList());
                    
                    return buildZoneAnalytics(zone, zoneShipments);
                })
                .filter(analytics -> analytics.getShipmentCount() > 0)
                .sorted(Comparator.comparing(ReportsResponse.ZoneAnalytics::getShipmentCount).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public ReportsResponse.RevenueAnalytics getRevenueAnalytics(LocalDate startDate, LocalDate endDate, TAccountRequest accountRequest) {
        log.info("Generating revenue analytics report from {} to {} for account: {}", startDate, endDate, accountRequest.username());
        
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        List<EShipment> shipments = shipmentRepository.findByCreatedAtBetween(startInstant, endInstant);
        
        // Calculate total revenue components
        BigDecimal totalRevenue = shipments.stream()
                .map(s -> s.getShippingCost().add(s.getInsuranceCost() != null ? s.getInsuranceCost() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal shippingRevenue = shipments.stream()
                .map(EShipment::getShippingCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal insuranceRevenue = shipments.stream()
                .map(s -> s.getInsuranceCost() != null ? s.getInsuranceCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Revenue by carrier
        Map<String, BigDecimal> revenueByCarrier = shipments.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCarrier().getCarrierName(),
                        Collectors.reducing(BigDecimal.ZERO, EShipment::getShippingCost, BigDecimal::add)
                ));
        
        // Revenue by service type
        Map<String, BigDecimal> revenueByServiceType = shipments.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getMethod().getServiceType().name(),
                        Collectors.reducing(BigDecimal.ZERO, EShipment::getShippingCost, BigDecimal::add)
                ));
        
        // Monthly breakdown
        List<ReportsResponse.RevenueAnalytics.MonthlyRevenue> monthlyBreakdown = 
                generateMonthlyRevenueBreakdown(shipments, startDate, endDate);
        
        return ReportsResponse.RevenueAnalytics.builder()
                .period(startDate + " to " + endDate)
                .totalRevenue(totalRevenue)
                .shippingRevenue(shippingRevenue)
                .insuranceRevenue(insuranceRevenue)
                .revenueByCarrier(revenueByCarrier)
                .revenueByServiceType(revenueByServiceType)
                .monthlyBreakdown(monthlyBreakdown)
                .build();
    }

    @Override
    public ReportsResponse.OperationalMetrics getOperationalMetrics(TAccountRequest accountRequest) {
        log.info("Generating operational metrics report for account: {}", accountRequest.username());
        
        // Get current status counts
        long totalActiveShipments = shipmentRepository.countByStatus(ShipmentStatus.IN_TRANSIT) +
                                   shipmentRepository.countByStatus(ShipmentStatus.OUT_FOR_DELIVERY) +
                                   shipmentRepository.countByStatus(ShipmentStatus.PICKED_UP);
        
        long pendingShipments = shipmentRepository.countByStatus(ShipmentStatus.PENDING);
        long inTransitShipments = shipmentRepository.countByStatus(ShipmentStatus.IN_TRANSIT);
        
        // Today's deliveries
        LocalDate today = LocalDate.now();
        List<EShipment> todayDeliveries = shipmentRepository.findByActualDeliveryDateBetween(today, today);
        long deliveredToday = todayDeliveries.size();
        
        long failedDeliveries = shipmentRepository.countByStatus(ShipmentStatus.FAILED);
        
        // Calculate average processing time (from creation to shipped)
        List<EShipment> shippedShipments = shipmentRepository.findByStatus(ShipmentStatus.DELIVERED);
        double averageProcessingTime = shippedShipments.stream()
                .filter(s -> s.getShippedAt() != null)
                .mapToDouble(s -> ChronoUnit.HOURS.between(s.getCreatedAt(), s.getShippedAt()))
                .average()
                .orElse(0.0);
        
        // Peak shipping hours (simulated - in real implementation would analyze historical data)
        List<Integer> peakShippingHours = Arrays.asList(9, 10, 11, 14, 15, 16);
        
        // Webhook processing rate
        List<EShippingWebhook> allWebhooks = webhookRepository.findAll();
        long processedWebhooks = allWebhooks.stream()
                .mapToLong(w -> w.getProcessed() ? 1 : 0)
                .sum();
        
        double webhookProcessingRate = allWebhooks.isEmpty() ? 100.0 : 
                (double) processedWebhooks / allWebhooks.size() * 100;
        
        return ReportsResponse.OperationalMetrics.builder()
                .totalActiveShipments(totalActiveShipments)
                .pendingShipments(pendingShipments)
                .inTransitShipments(inTransitShipments)
                .deliveredToday(deliveredToday)
                .failedDeliveries(failedDeliveries)
                .averageProcessingTime(averageProcessingTime)
                .peakShippingHours(peakShippingHours)
                .webhookProcessingRate(webhookProcessingRate)
                .build();
    }

    @Override
    public ReportsResponse.CustomerInsights getCustomerInsights(LocalDate startDate, LocalDate endDate, TAccountRequest accountRequest) {
        log.info("Generating customer insights report from {} to {} for account: {}", startDate, endDate, accountRequest.username());
        
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        List<EShipment> shipments = shipmentRepository.findByCreatedAtBetween(startInstant, endInstant);
        
        // Top destinations (simulated based on shipment data)
        List<ReportsResponse.CustomerInsights.Destination> topDestinations = generateTopDestinations(shipments);
        
        // Preferred service types
        Map<String, Long> preferredServiceTypes = shipments.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getMethod().getServiceType().name(),
                        Collectors.counting()
                ));
        
        // Average package weight
        BigDecimal averagePackageWeight = shipments.stream()
                .map(EShipment::getTotalWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(shipments.size(), 1)), 2, RoundingMode.HALF_UP);
        
        // Repeat customer rate (simulated - would need customer data)
        double repeatCustomerRate = 65.0; // Example rate
        
        // Customer acquisition cost (simulated)
        BigDecimal customerAcquisitionCost = new BigDecimal("25.50");
        
        return ReportsResponse.CustomerInsights.builder()
                .topDestinations(topDestinations)
                .preferredServiceTypes(preferredServiceTypes)
                .averagePackageWeight(averagePackageWeight)
                .repeatCustomerRate(repeatCustomerRate)
                .customerAcquisitionCost(customerAcquisitionCost)
                .build();
    }

    @Override
    public ReportsResponse.TrendAnalysis getTrendAnalysis(String metricName, LocalDate startDate, LocalDate endDate, TAccountRequest accountRequest) {
        log.info("Generating trend analysis for metric '{}' from {} to {} for account: {}", metricName, startDate, endDate, accountRequest.username());
        
        List<ReportsResponse.TrendAnalysis.DataPoint> dataPoints = generateTrendDataPoints(metricName, startDate, endDate);
        
        // Calculate trend direction
        String trendDirection = calculateTrendDirection(dataPoints);
        
        // Calculate percentage change
        double percentageChange = calculatePercentageChange(dataPoints);
        
        // Generate forecast (simple linear projection)
        List<ReportsResponse.TrendAnalysis.DataPoint> forecast = generateForecast(dataPoints, 30);
        
        return ReportsResponse.TrendAnalysis.builder()
                .metricName(metricName)
                .trendDirection(trendDirection)
                .percentageChange(percentageChange)
                .dataPoints(dataPoints)
                .forecast(forecast)
                .build();
    }

    private ReportsResponse.CarrierPerformance buildCarrierPerformance(EShippingCarrier carrier, List<EShipment> shipments) {
        if (shipments.isEmpty()) {
            return ReportsResponse.CarrierPerformance.builder()
                    .carrierId(carrier.getCarrierId())
                    .carrierName(carrier.getCarrierName())
                    .shipmentCount(0L)
                    .onTimeDeliveryRate(0.0)
                    .averageDeliveryDays(0.0)
                    .totalRevenue(BigDecimal.ZERO)
                    .costPerShipment(BigDecimal.ZERO)
                    .customerSatisfaction(0.0)
                    .build();
        }
        
        // Calculate on-time delivery rate
        List<EShipment> deliveredShipments = shipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.DELIVERED && 
                            s.getEstimatedDeliveryDate() != null && 
                            s.getActualDeliveryDate() != null)
                .collect(Collectors.toList());
        
        long onTimeDeliveries = deliveredShipments.stream()
                .mapToLong(s -> s.getActualDeliveryDate().isAfter(s.getEstimatedDeliveryDate()) ? 0 : 1)
                .sum();
        
        double onTimeDeliveryRate = deliveredShipments.isEmpty() ? 0.0 : 
                (double) onTimeDeliveries / deliveredShipments.size() * 100;
        
        // Calculate average delivery days
        double averageDeliveryDays = deliveredShipments.stream()
                .filter(s -> s.getShippedAt() != null)
                .mapToLong(s -> ChronoUnit.DAYS.between(
                        s.getShippedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                        s.getActualDeliveryDate()))
                .average()
                .orElse(0.0);
        
        // Calculate revenue metrics
        BigDecimal totalRevenue = shipments.stream()
                .map(EShipment::getShippingCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal costPerShipment = totalRevenue.divide(
                BigDecimal.valueOf(shipments.size()), 2, RoundingMode.HALF_UP);
        
        // Customer satisfaction (simulated)
        double customerSatisfaction = onTimeDeliveryRate * 0.8 + 20; // Simple simulation
        
        return ReportsResponse.CarrierPerformance.builder()
                .carrierId(carrier.getCarrierId())
                .carrierName(carrier.getCarrierName())
                .shipmentCount((long) shipments.size())
                .onTimeDeliveryRate(onTimeDeliveryRate)
                .averageDeliveryDays(averageDeliveryDays)
                .totalRevenue(totalRevenue)
                .costPerShipment(costPerShipment)
                .customerSatisfaction(customerSatisfaction)
                .build();
    }

    private ReportsResponse.ZoneAnalytics buildZoneAnalytics(EShippingZone zone, List<EShipment> shipments) {
        if (shipments.isEmpty()) {
            return ReportsResponse.ZoneAnalytics.builder()
                    .zoneId(zone.getZoneId())
                    .zoneName(zone.getZoneName())
                    .shipmentCount(0L)
                    .averageShippingCost(BigDecimal.ZERO)
                    .mostPopularServiceType("N/A")
                    .deliverySuccessRate(0.0)
                    .build();
        }
        
        // Calculate average shipping cost
        BigDecimal averageShippingCost = shipments.stream()
                .map(EShipment::getShippingCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(shipments.size()), 2, RoundingMode.HALF_UP);
        
        // Find most popular service type
        String mostPopularServiceType = shipments.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getMethod().getServiceType().name(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        
        // Calculate delivery success rate
        long successfulDeliveries = shipments.stream()
                .mapToLong(s -> s.getStatus() == ShipmentStatus.DELIVERED ? 1 : 0)
                .sum();
        
        double deliverySuccessRate = (double) successfulDeliveries / shipments.size() * 100;
        
        return ReportsResponse.ZoneAnalytics.builder()
                .zoneId(zone.getZoneId())
                .zoneName(zone.getZoneName())
                .shipmentCount((long) shipments.size())
                .averageShippingCost(averageShippingCost)
                .mostPopularServiceType(mostPopularServiceType)
                .deliverySuccessRate(deliverySuccessRate)
                .build();
    }

    private List<ReportsResponse.RevenueAnalytics.MonthlyRevenue> generateMonthlyRevenueBreakdown(
            List<EShipment> shipments, LocalDate startDate, LocalDate endDate) {
        
        Map<String, List<EShipment>> shipmentsByMonth = shipments.stream()
                .collect(Collectors.groupingBy(s -> 
                        s.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString().substring(0, 7)
                ));
        
        return shipmentsByMonth.entrySet().stream()
                .map(entry -> {
                    String month = entry.getKey();
                    List<EShipment> monthShipments = entry.getValue();
                    
                    BigDecimal monthRevenue = monthShipments.stream()
                            .map(EShipment::getShippingCost)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return ReportsResponse.RevenueAnalytics.MonthlyRevenue.builder()
                            .month(month)
                            .revenue(monthRevenue)
                            .shipmentCount((long) monthShipments.size())
                            .build();
                })
                .sorted(Comparator.comparing(ReportsResponse.RevenueAnalytics.MonthlyRevenue::getMonth))
                .collect(Collectors.toList());
    }

    private List<ReportsResponse.CustomerInsights.Destination> generateTopDestinations(List<EShipment> shipments) {
        // Simulate top destinations (in real implementation, would parse addresses)
        Map<String, Long> destinationCounts = Map.of(
                "New York, NY", 150L,
                "Los Angeles, CA", 120L,
                "Chicago, IL", 95L,
                "Houston, TX", 80L,
                "Phoenix, AZ", 65L
        );
        
        long totalShipments = shipments.size();
        
        return destinationCounts.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split(", ");
                    return ReportsResponse.CustomerInsights.Destination.builder()
                            .country("USA")
                            .city(parts[0])
                            .shipmentCount(entry.getValue())
                            .percentage(totalShipments > 0 ? 
                                    (double) entry.getValue() / totalShipments * 100 : 0.0)
                            .build();
                })
                .sorted(Comparator.comparing(ReportsResponse.CustomerInsights.Destination::getShipmentCount).reversed())
                .collect(Collectors.toList());
    }

    private List<ReportsResponse.TrendAnalysis.DataPoint> generateTrendDataPoints(String metricName, LocalDate startDate, LocalDate endDate) {
        List<ReportsResponse.TrendAnalysis.DataPoint> dataPoints = new ArrayList<>();
        
        // Generate daily data points for the date range
        LocalDate currentDate = startDate;
        Random random = new Random();
        
        while (!currentDate.isAfter(endDate)) {
            BigDecimal value = generateMetricValue(metricName, currentDate, random);
            
            dataPoints.add(ReportsResponse.TrendAnalysis.DataPoint.builder()
                    .date(currentDate)
                    .value(value)
                    .build());
            
            currentDate = currentDate.plusDays(1);
        }
        
        return dataPoints;
    }

    private BigDecimal generateMetricValue(String metricName, LocalDate date, Random random) {
        // Simulate different metric patterns
        return switch (metricName.toLowerCase()) {
            case "shipments" -> BigDecimal.valueOf(50 + random.nextInt(100));
            case "revenue" -> BigDecimal.valueOf(1000 + random.nextInt(5000));
            case "delivery_time" -> BigDecimal.valueOf(2 + random.nextDouble() * 3);
            default -> BigDecimal.valueOf(random.nextInt(100));
        };
    }

    private String calculateTrendDirection(List<ReportsResponse.TrendAnalysis.DataPoint> dataPoints) {
        if (dataPoints.size() < 2) {
            return "STABLE";
        }
        
        BigDecimal firstValue = dataPoints.get(0).getValue();
        BigDecimal lastValue = dataPoints.get(dataPoints.size() - 1).getValue();
        
        if (lastValue.compareTo(firstValue) > 0) {
            return "UP";
        } else if (lastValue.compareTo(firstValue) < 0) {
            return "DOWN";
        } else {
            return "STABLE";
        }
    }

    private double calculatePercentageChange(List<ReportsResponse.TrendAnalysis.DataPoint> dataPoints) {
        if (dataPoints.size() < 2) {
            return 0.0;
        }
        
        BigDecimal firstValue = dataPoints.get(0).getValue();
        BigDecimal lastValue = dataPoints.get(dataPoints.size() - 1).getValue();
        
        if (firstValue.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        
        return lastValue.subtract(firstValue)
                .divide(firstValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private List<ReportsResponse.TrendAnalysis.DataPoint> generateForecast(List<ReportsResponse.TrendAnalysis.DataPoint> dataPoints, int forecastDays) {
        if (dataPoints.size() < 2) {
            return new ArrayList<>();
        }
        
        // Simple linear regression for forecast
        List<ReportsResponse.TrendAnalysis.DataPoint> forecast = new ArrayList<>();
        LocalDate lastDate = dataPoints.get(dataPoints.size() - 1).getDate();
        BigDecimal lastValue = dataPoints.get(dataPoints.size() - 1).getValue();
        
        // Calculate simple trend (difference between last two points)
        BigDecimal trend = lastValue.subtract(dataPoints.get(dataPoints.size() - 2).getValue());
        
        for (int i = 1; i <= forecastDays; i++) {
            LocalDate forecastDate = lastDate.plusDays(i);
            BigDecimal forecastValue = lastValue.add(trend.multiply(BigDecimal.valueOf(i)));
            
            forecast.add(ReportsResponse.TrendAnalysis.DataPoint.builder()
                    .date(forecastDate)
                    .value(forecastValue.max(BigDecimal.ZERO)) // Ensure non-negative values
                    .build());
        }
        
        return forecast;
    }
} 
