package com.winnguyen1905.shipping.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.winnguyen1905.shipping.core.model.response.ReportsResponse;
import com.winnguyen1905.shipping.core.service.ReportsService;
import com.winnguyen1905.shipping.secure.AccountRequest;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports & Analytics", description = "APIs for shipping analytics and reporting")
public class ReportsController {

    @Autowired
    private ReportsService reportsService;

    @GetMapping("/shipping-overview")
    @Operation(summary = "Get shipping overview", 
               description = "Retrieves shipping overview for a date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Overview retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ReportsResponse.ShippingOverview> getShippingOverview(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AccountRequest TAccountRequest accountRequest) {
        ReportsResponse.ShippingOverview response = reportsService.getShippingOverview(startDate, endDate, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/carrier-performance")
    @Operation(summary = "Get carrier performance", 
               description = "Retrieves carrier performance analytics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Performance data retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ReportsResponse.CarrierPerformance>> getCarrierPerformance(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AccountRequest TAccountRequest accountRequest) {
        List<ReportsResponse.CarrierPerformance> response = reportsService.getCarrierPerformance(startDate, endDate, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/zone-analytics")
    @Operation(summary = "Get zone analytics", 
               description = "Retrieves zone analytics data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Zone analytics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ReportsResponse.ZoneAnalytics>> getZoneAnalytics(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AccountRequest TAccountRequest accountRequest) {
        List<ReportsResponse.ZoneAnalytics> response = reportsService.getZoneAnalytics(startDate, endDate, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue-analytics")
    @Operation(summary = "Get revenue analytics", 
               description = "Retrieves revenue analytics data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Revenue analytics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ReportsResponse.RevenueAnalytics> getRevenueAnalytics(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AccountRequest TAccountRequest accountRequest) {
        ReportsResponse.RevenueAnalytics response = reportsService.getRevenueAnalytics(startDate, endDate, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operational-metrics")
    @Operation(summary = "Get operational metrics", 
               description = "Retrieves current operational metrics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operational metrics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ReportsResponse.OperationalMetrics> getOperationalMetrics(
            @AccountRequest TAccountRequest accountRequest) {
        ReportsResponse.OperationalMetrics response = reportsService.getOperationalMetrics(accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer-insights")
    @Operation(summary = "Get customer insights", 
               description = "Retrieves customer behavior insights")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer insights retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ReportsResponse.CustomerInsights> getCustomerInsights(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AccountRequest TAccountRequest accountRequest) {
        ReportsResponse.CustomerInsights response = reportsService.getCustomerInsights(startDate, endDate, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trend-analysis")
    @Operation(summary = "Get trend analysis", 
               description = "Retrieves trend analysis for a specific metric")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trend analysis retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ReportsResponse.TrendAnalysis> getTrendAnalysis(
            @Parameter(description = "Metric name") @RequestParam String metricName,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AccountRequest TAccountRequest accountRequest) {
        ReportsResponse.TrendAnalysis response = reportsService.getTrendAnalysis(metricName, startDate, endDate, accountRequest);
        return ResponseEntity.ok(response);
    }
} 
