package com.winnguyen1905.shipping.core.provider.impl;

import com.winnguyen1905.shipping.core.model.request.ShippingQuoteRequest;
import com.winnguyen1905.shipping.core.model.response.ShippingQuoteResponse;
import com.winnguyen1905.shipping.core.provider.ShippingProviderClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * EasyPost implementation of ShippingProviderClient
 * EasyPost is a shipping API aggregator that provides access to multiple
 * carriers
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EasyPostShippingClient implements ShippingProviderClient {

  private final RestTemplate restTemplate;

  @Value("${shipping.providers.easypost.api-key:#{null}}")
  private String apiKey;

  @Value("${shipping.providers.easypost.base-url:https://api.easypost.com/v2}")
  private String baseUrl;

  @Value("${shipping.providers.easypost.enabled:false}")
  private boolean enabled;

  @Override
  public List<ShippingQuoteResponse.ShippingOption> getShippingQuotes(ShippingQuoteRequest request) {
    if (!enabled || !isAvailable()) {
      log.warn("EasyPost provider is not available");
      return Collections.emptyList();
    }

    try {
      log.info("Getting shipping quotes from EasyPost for request: {}", request);

      // Validate request
      if (!supportsRoute(request.getVendor().getAddress().getCountry(),
          request.getCustomer().getAddress().getCountry())) {
        log.warn("EasyPost does not support route from {} to {}",
            request.getVendor().getAddress().getCountry(),
            request.getCustomer().getAddress().getCountry());
        return Collections.emptyList();
      }

      // Create EasyPost shipment request
      Map<String, Object> shipmentRequest = buildEasyPostRequest(request);

      // Make API call to EasyPost
      // This is a placeholder - in real implementation, you would make HTTP calls
      Map<String, Object> response = callEasyPostAPI(shipmentRequest);

      // Parse response and convert to our format
      return parseEasyPostResponse(response, request);

    } catch (Exception e) {
      log.error("Error getting quotes from EasyPost", e);
      return Collections.emptyList();
    }
  }

  @Override
  public Map<String, List<ShippingQuoteResponse.ShippingOption>> getMultiVendorQuotes(ShippingQuoteRequest request) {
    if (!request.isMultiVendorRequest()) {
      return Collections.emptyMap();
    }

    Map<String, List<ShippingQuoteResponse.ShippingOption>> result = new HashMap<>();

    for (ShippingQuoteRequest.VendorPackageInfo vendorPackage : request.getVendorPackages()) {
      // Create single vendor request for each vendor
      ShippingQuoteRequest singleVendorRequest = ShippingQuoteRequest.builder()
          .customer(request.getCustomer())
          .vendor(vendorPackage.getVendor())
          .packageInfo(vendorPackage.getPackageInfo())
          .requireInsurance(request.getRequireInsurance())
          .preferredCurrency(request.getPreferredCurrency())
          .build();

      List<ShippingQuoteResponse.ShippingOption> options = getShippingQuotes(singleVendorRequest);
      if (!options.isEmpty()) {
        result.put(vendorPackage.getVendor().getVendorId(), options);
      }
    }

    return result;
  }

  @Override
  public boolean supportsRoute(String originCountry, String destinationCountry) {
    // EasyPost supports many international routes
    List<String> supportedCountries = getSupportedCountries();
    return supportedCountries.contains(originCountry) && supportedCountries.contains(destinationCountry);
  }

  @Override
  public boolean supportsPackage(BigDecimal weight, ShippingQuoteRequest.DimensionsInfo dimensions,
      BigDecimal declaredValue) {
    // Check weight limits
    if (weight.compareTo(getMaxPackageWeight()) > 0) {
      return false;
    }

    // Check dimension limits (example limits)
    BigDecimal maxDimension = new BigDecimal("150"); // 150 cm
    if (dimensions.getLength().compareTo(maxDimension) > 0 ||
        dimensions.getWidth().compareTo(maxDimension) > 0 ||
        dimensions.getHeight().compareTo(maxDimension) > 0) {
      return false;
    }

    // Check declared value limits
    return declaredValue.compareTo(getMaxDeclaredValue()) <= 0;
  }

  @Override
  public String getProviderName() {
    return "EasyPost";
  }

  @Override
  public List<String> getSupportedCountries() {
    // EasyPost supports many countries through different carriers
    return List.of("US", "CA", "GB", "AU", "DE", "FR", "JP", "VN", "TH", "SG", "MY", "ID", "PH");
  }

  @Override
  public BigDecimal getMaxPackageWeight() {
    return new BigDecimal("70"); // 70 kg limit for most carriers
  }

  @Override
  public BigDecimal getMaxDeclaredValue() {
    return new BigDecimal("50000"); // $50,000 USD equivalent
  }

  @Override
  public boolean isAvailable() {
    return enabled && apiKey != null && !apiKey.trim().isEmpty();
  }

  @Override
  public Map<String, Object> getProviderConfiguration() {
    Map<String, Object> config = new HashMap<>();
    config.put("provider", "EasyPost");
    config.put("enabled", enabled);
    config.put("baseUrl", baseUrl);
    config.put("hasApiKey", apiKey != null && !apiKey.trim().isEmpty());
    config.put("maxWeight", getMaxPackageWeight());
    config.put("maxValue", getMaxDeclaredValue());
    return config;
  }

  // Private helper methods

  private Map<String, Object> buildEasyPostRequest(ShippingQuoteRequest request) {
    Map<String, Object> shipment = new HashMap<>();

    // To address
    Map<String, Object> toAddress = new HashMap<>();
    toAddress.put("name", request.getCustomer().getName());
    toAddress.put("street1", request.getCustomer().getAddress().getStreet());
    toAddress.put("city", request.getCustomer().getAddress().getCity());
    toAddress.put("state", request.getCustomer().getAddress().getState());
    toAddress.put("zip", request.getCustomer().getAddress().getZip());
    toAddress.put("country", request.getCustomer().getAddress().getCountry());

    // From address
    Map<String, Object> fromAddress = new HashMap<>();
    fromAddress.put("name", request.getVendor().getName());
    fromAddress.put("street1", request.getVendor().getAddress().getStreet());
    fromAddress.put("city", request.getVendor().getAddress().getCity());
    fromAddress.put("state", request.getVendor().getAddress().getState());
    fromAddress.put("zip", request.getVendor().getAddress().getZip());
    fromAddress.put("country", request.getVendor().getAddress().getCountry());

    // Parcel
    Map<String, Object> parcel = new HashMap<>();
    parcel.put("weight", request.getPackageInfo().getWeight());
    parcel.put("length", request.getPackageInfo().getDimensions().getLength());
    parcel.put("width", request.getPackageInfo().getDimensions().getWidth());
    parcel.put("height", request.getPackageInfo().getDimensions().getHeight());

    shipment.put("to_address", toAddress);
    shipment.put("from_address", fromAddress);
    shipment.put("parcel", parcel);

    Map<String, Object> request_body = new HashMap<>();
    request_body.put("shipment", shipment);

    return request_body;
  }

  private Map<String, Object> callEasyPostAPI(Map<String, Object> request) {
    // This is a placeholder for the actual API call
    // In a real implementation, you would:
    // 1. Set up HTTP headers with API key
    // 2. Make POST request to EasyPost API
    // 3. Handle authentication, rate limiting, errors

    log.info("Making API call to EasyPost (placeholder)");

    // Simulate API response
    return createMockEasyPostResponse();
  }

  private Map<String, Object> createMockEasyPostResponse() {
    // Mock response simulating EasyPost API format
    Map<String, Object> response = new HashMap<>();

    List<Map<String, Object>> rates = new ArrayList<>();

    // Mock DHL rate
    Map<String, Object> dhlRate = new HashMap<>();
    dhlRate.put("id", "rate_dhl_123");
    dhlRate.put("service", "Express");
    dhlRate.put("carrier", "DHL");
    dhlRate.put("rate", "25.50");
    dhlRate.put("currency", "USD");
    dhlRate.put("delivery_days", 2);
    rates.add(dhlRate);

    // Mock FedEx rate
    Map<String, Object> fedexRate = new HashMap<>();
    fedexRate.put("id", "rate_fedex_456");
    fedexRate.put("service", "Ground");
    fedexRate.put("carrier", "FedEx");
    fedexRate.put("rate", "18.75");
    fedexRate.put("currency", "USD");
    fedexRate.put("delivery_days", 3);
    rates.add(fedexRate);

    response.put("rates", rates);
    return response;
  }

  @SuppressWarnings("unchecked")
  private List<ShippingQuoteResponse.ShippingOption> parseEasyPostResponse(Map<String, Object> response,
      ShippingQuoteRequest request) {
    List<ShippingQuoteResponse.ShippingOption> options = new ArrayList<>();

    List<Map<String, Object>> rates = (List<Map<String, Object>>) response.get("rates");
    if (rates == null) {
      return options;
    }

    for (Map<String, Object> rate : rates) {
      try {
        ShippingQuoteResponse.ShippingOption option = ShippingQuoteResponse.ShippingOption.builder()
            .provider((String) rate.get("carrier"))
            .service((String) rate.get("service"))
            .cost(new BigDecimal((String) rate.get("rate")))
            .currency((String) rate.get("currency"))
            .estimatedDays((Integer) rate.get("delivery_days"))
            .estimatedDeliveryDate(LocalDate.now().plusDays((Integer) rate.get("delivery_days")))
            .serviceCode((String) rate.get("id"))
            .trackingSupported("Yes")
            .insuranceIncluded(false)
            .deliveryType("DOOR_TO_DOOR")
            .features(List.of("Tracking", "Insurance available"))
            .rating(ShippingQuoteResponse.ProviderRating.builder()
                .overallRating(4.2)
                .deliveryTimeRating(4.5)
                .reliabilityRating(4.0)
                .customerServiceRating(4.1)
                .totalReviews(1250)
                .build())
            .build();

        options.add(option);
      } catch (Exception e) {
        log.error("Error parsing rate from EasyPost response", e);
      }
    }

    return options;
  }
}
