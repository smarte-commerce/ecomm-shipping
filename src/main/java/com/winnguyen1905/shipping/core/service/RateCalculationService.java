package com.winnguyen1905.shipping.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.winnguyen1905.shipping.core.model.request.CalculateRateRequest;
import com.winnguyen1905.shipping.core.model.response.RateCalculationResponse;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import java.util.List;

public interface RateCalculationService {

    /**
     * Calculates shipping rates for given shipment details
     * @param request The rate calculation request
     * @param accountRequest The account request for authorization
     * @return The rate calculation response with available rates
     */
    RateCalculationResponse calculateRates(CalculateRateRequest request, TAccountRequest accountRequest);

    /**
     * Retrieves a rate calculation by its ID
     * @param id The rate calculation ID
     * @param accountRequest The account request for authorization
     * @return The rate calculation response
     */
    RateCalculationResponse getRateCalculationById(Long id, TAccountRequest accountRequest);

    /**
     * Retrieves all rate calculations with optional filtering
     * @param orderId Filter by order ID
     * @param fromZip Filter by from ZIP code
     * @param toZip Filter by to ZIP code
     * @param pageable Pagination parameters
     * @param accountRequest The account request for authorization
     * @return Paginated list of rate calculation responses
     */
    Page<RateCalculationResponse> getAllRateCalculations(Long orderId, String fromZip, String toZip, 
                                                        Pageable pageable, TAccountRequest accountRequest);

    /**
     * Retrieves all rate calculations for a specific order
     * @param orderId The order ID
     * @param accountRequest The account request for authorization
     * @return List of rate calculation responses
     */
    List<RateCalculationResponse> getRateCalculationsByOrderId(Long orderId, TAccountRequest accountRequest);

    /**
     * Provides a quick rate estimate without saving the calculation
     * @param request The rate calculation request
     * @param accountRequest The account request for authorization
     * @return The rate calculation response
     */
    RateCalculationResponse quickRateEstimate(CalculateRateRequest request, TAccountRequest accountRequest);

    /**
     * Calculates rates for multiple shipments in a single request
     * @param requests List of rate calculation requests
     * @param accountRequest The account request for authorization
     * @return List of rate calculation responses
     */
    List<RateCalculationResponse> bulkCalculateRates(List<CalculateRateRequest> requests, TAccountRequest accountRequest);

    /**
     * Compares rates across all available carriers for given shipment details
     * @param request The rate calculation request
     * @param accountRequest The account request for authorization
     * @return The rate calculation response with comparison data
     */
    RateCalculationResponse compareRates(CalculateRateRequest request, TAccountRequest accountRequest);
} 
