package com.winnguyen1905.shipping.core.provider;

import com.winnguyen1905.shipping.core.model.request.ShippingQuoteRequest;
import com.winnguyen1905.shipping.core.model.response.ShippingQuoteResponse;

import java.util.List;
import java.util.Map;

/**
 * Abstract interface for third-party shipping provider clients
 * Allows swapping between different shipping APIs (DHL, FedEx, VNPost, etc.)
 */
public interface ShippingProviderClient {

  /**
   * Get shipping quotes for a single vendor shipment
   * 
   * @param request The shipping quote request
   * @return List of shipping options from this provider
   */
  List<ShippingQuoteResponse.ShippingOption> getShippingQuotes(ShippingQuoteRequest request);

  /**
   * Get shipping quotes for multiple vendors (consolidated)
   * 
   * @param request The shipping quote request with multiple vendors
   * @return Map of vendor ID to shipping options
   */
  Map<String, List<ShippingQuoteResponse.ShippingOption>> getMultiVendorQuotes(ShippingQuoteRequest request);

  /**
   * Validate if the provider supports shipping from origin to destination
   * 
   * @param originCountry      Origin country code
   * @param destinationCountry Destination country code
   * @return true if route is supported
   */
  boolean supportsRoute(String originCountry, String destinationCountry);

  /**
   * Check if provider supports the given package specifications
   * 
   * @param weight        Package weight in kg
   * @param dimensions    Package dimensions in cm
   * @param declaredValue Package declared value
   * @return true if package is supported
   */
  boolean supportsPackage(java.math.BigDecimal weight,
      ShippingQuoteRequest.DimensionsInfo dimensions,
      java.math.BigDecimal declaredValue);

  /**
   * Get the provider name/identifier
   * 
   * @return Provider name (e.g., "DHL", "VNPost", "GHN")
   */
  String getProviderName();

  /**
   * Get the provider's supported countries
   * 
   * @return List of supported country codes
   */
  List<String> getSupportedCountries();

  /**
   * Get the provider's maximum package weight limit in kg
   * 
   * @return Maximum weight or null if no limit
   */
  java.math.BigDecimal getMaxPackageWeight();

  /**
   * Get the provider's maximum declared value limit
   * 
   * @return Maximum declared value or null if no limit
   */
  java.math.BigDecimal getMaxDeclaredValue();

  /**
   * Check if the provider is currently available/operational
   * 
   * @return true if provider is available
   */
  boolean isAvailable();

  /**
   * Get provider-specific configuration or settings
   * 
   * @return Configuration map
   */
  Map<String, Object> getProviderConfiguration();

  /**
   * Track a shipment using provider's tracking API
   * 
   * @param trackingNumber The tracking number
   * @return Tracking information or null if not supported
   */
  default TrackingInfo trackShipment(String trackingNumber) {
    return null; // Default implementation - not all providers may support tracking via this
                 // interface
  }

  /**
   * Create a shipment label with the provider
   * 
   * @param request        The shipping request
   * @param selectedOption The selected shipping option
   * @return Shipment creation response
   */
  default ShipmentCreationResponse createShipment(ShippingQuoteRequest request,
      ShippingQuoteResponse.ShippingOption selectedOption) {
    throw new UnsupportedOperationException("Shipment creation not supported by this provider");
  }

  /**
   * Calculate dimensional weight based on provider's formula
   * 
   * @param dimensions Package dimensions
   * @return Dimensional weight in kg
   */
  default java.math.BigDecimal calculateDimensionalWeight(ShippingQuoteRequest.DimensionsInfo dimensions) {
    // Standard formula: (L x W x H) / 5000 for cm to kg
    return dimensions.getLength()
        .multiply(dimensions.getWidth())
        .multiply(dimensions.getHeight())
        .divide(new java.math.BigDecimal("5000"), 2, java.math.RoundingMode.HALF_UP);
  }

  // Supporting data classes
  record TrackingInfo(
      String trackingNumber,
      String status,
      String location,
      java.time.Instant lastUpdate,
      java.time.LocalDate estimatedDelivery,
      List<TrackingEvent> events) {
  }

  record TrackingEvent(
      java.time.Instant timestamp,
      String status,
      String location,
      String description) {
  }

  record ShipmentCreationResponse(
      String shipmentId,
      String trackingNumber,
      String labelUrl,
      java.math.BigDecimal actualCost,
      String currency,
      boolean success,
      String errorMessage) {
  }
}
