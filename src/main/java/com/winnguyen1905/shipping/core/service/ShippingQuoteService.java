package com.winnguyen1905.shipping.core.service;

import com.winnguyen1905.shipping.core.model.request.ShippingQuoteRequest;
import com.winnguyen1905.shipping.core.model.response.ShippingQuoteResponse;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing shipping quotes from multiple providers
 */
public interface ShippingQuoteService {

  /**
   * Get shipping quotes for a single vendor shipment
   * 
   * @param request        The shipping quote request
   * @param accountRequest The account request for authorization
   * @return Shipping quote response with options from all available providers
   */
  ShippingQuoteResponse getShippingQuotes(ShippingQuoteRequest request, TAccountRequest accountRequest);

  /**
   * Get shipping quotes for multiple vendors (consolidated cart)
   * 
   * @param request        The multi-vendor shipping quote request
   * @param accountRequest The account request for authorization
   * @return Consolidated shipping quote response
   */
  ShippingQuoteResponse getMultiVendorShippingQuotes(ShippingQuoteRequest request, TAccountRequest accountRequest);

  /**
   * Get cached shipping quotes if available
   * 
   * @param request        The shipping quote request
   * @param accountRequest The account request for authorization
   * @return Cached shipping quote response or null if not found
   */
  ShippingQuoteResponse getCachedQuotes(ShippingQuoteRequest request, TAccountRequest accountRequest);

  /**
   * Save shipping quotes to cache
   * 
   * @param request        The shipping quote request
   * @param response       The shipping quote response
   * @param accountRequest The account request for authorization
   */
  void cacheQuotes(ShippingQuoteRequest request, ShippingQuoteResponse response, TAccountRequest accountRequest);

  /**
   * Get quotes from a specific provider only
   * 
   * @param request        The shipping quote request
   * @param providerName   The name of the provider
   * @param accountRequest The account request for authorization
   * @return List of shipping options from the specified provider
   */
  List<ShippingQuoteResponse.ShippingOption> getQuotesFromProvider(
      ShippingQuoteRequest request, String providerName, TAccountRequest accountRequest);

  /**
   * Get all available providers for a route
   * 
   * @param originCountry      Origin country code
   * @param destinationCountry Destination country code
   * @return List of available provider names
   */
  List<String> getAvailableProviders(String originCountry, String destinationCountry);

  /**
   * Validate shipping quote request
   * 
   * @param request The shipping quote request
   * @return Validation result with any errors
   */
  ValidationResult validateShippingQuoteRequest(ShippingQuoteRequest request);

  /**
   * Get provider configurations and status
   * 
   * @return Map of provider name to configuration details
   */
  Map<String, Object> getProviderStatus();

  /**
   * Test connectivity to all providers
   * 
   * @return Map of provider name to connectivity status
   */
  Map<String, Boolean> testProviderConnectivity();

  // Supporting classes
  record ValidationResult(
      boolean isValid,
      List<String> errors,
      List<String> warnings) {
  }
}
