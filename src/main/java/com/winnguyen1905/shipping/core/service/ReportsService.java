package com.winnguyen1905.shipping.core.service;

import com.winnguyen1905.shipping.core.model.response.ReportsResponse;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import java.time.LocalDate;
import java.util.List;

public interface ReportsService {

    /**
     * Gets shipping overview for a date range
     * @param startDate Start date
     * @param endDate End date
     * @param accountRequest The account request for authorization
     * @return Shipping overview report
     */
    ReportsResponse.ShippingOverview getShippingOverview(LocalDate startDate, LocalDate endDate, TAccountRequest accountRequest);

    /**
     * Gets carrier performance analytics
     * @param startDate Start date
     * @param endDate End date
     * @param accountRequest The account request for authorization
     * @return List of carrier performance reports
     */
    List<ReportsResponse.CarrierPerformance> getCarrierPerformance(LocalDate startDate, LocalDate endDate, TAccountRequest accountRequest);

    /**
     * Gets zone analytics
     * @param startDate Start date
     * @param endDate End date
     * @param accountRequest The account request for authorization
     * @return List of zone analytics reports
     */
    List<ReportsResponse.ZoneAnalytics> getZoneAnalytics(LocalDate startDate, LocalDate endDate, TAccountRequest accountRequest);

    /**
     * Gets revenue analytics
     * @param startDate Start date
     * @param endDate End date
     * @param accountRequest The account request for authorization
     * @return Revenue analytics report
     */
    ReportsResponse.RevenueAnalytics getRevenueAnalytics(LocalDate startDate, LocalDate endDate, TAccountRequest accountRequest);

    /**
     * Gets operational metrics
     * @param accountRequest The account request for authorization
     * @return Operational metrics report
     */
    ReportsResponse.OperationalMetrics getOperationalMetrics(TAccountRequest accountRequest);

    /**
     * Gets customer insights
     * @param startDate Start date
     * @param endDate End date
     * @param accountRequest The account request for authorization
     * @return Customer insights report
     */
    ReportsResponse.CustomerInsights getCustomerInsights(LocalDate startDate, LocalDate endDate, TAccountRequest accountRequest);

    /**
     * Gets trend analysis for a specific metric
     * @param metricName The metric to analyze
     * @param startDate Start date
     * @param endDate End date
     * @param accountRequest The account request for authorization
     * @return Trend analysis report
     */
    ReportsResponse.TrendAnalysis getTrendAnalysis(String metricName, LocalDate startDate, LocalDate endDate, TAccountRequest accountRequest);
} 
