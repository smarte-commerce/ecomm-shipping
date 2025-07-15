package com.winnguyen1905.shipping.core.controller;

import com.winnguyen1905.shipping.core.model.request.ShippingQuoteRequest;
import com.winnguyen1905.shipping.core.model.response.ShippingQuoteResponse;
import com.winnguyen1905.shipping.core.service.ShippingQuoteService;
import com.winnguyen1905.shipping.secure.AccountRequest;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/shipping/quotes")
@Tag(name = "Shipping Quotes", description = "APIs for getting shipping quotes from multiple providers")
@RequiredArgsConstructor
@Slf4j
public class ShippingQuoteController {

  private final ShippingQuoteService shippingQuoteService;

  @PostMapping("/calculate")
  @Operation(summary = "Calculate shipping quotes", description = "Get shipping quotes from all available providers for a single or multi-vendor shipment")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Quotes calculated successfully", content = @Content(schema = @Schema(implementation = ShippingQuoteResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<ShippingQuoteResponse> calculateShippingQuotes(
      @Valid @RequestBody ShippingQuoteRequest request,
      @AccountRequest TAccountRequest accountRequest) {

    log.info("Calculating shipping quotes for request type: {}",
        request.isSingleVendorRequest() ? "single-vendor" : "multi-vendor");

    ShippingQuoteResponse response;

    if (request.isSingleVendorRequest()) {
      response = shippingQuoteService.getShippingQuotes(request, accountRequest);
    } else {
      response = shippingQuoteService.getMultiVendorShippingQuotes(request, accountRequest);
    }

    return ResponseEntity.ok(response);
  }

  @PostMapping("/cart/review")
  @Operation(summary = "Review shipping costs for cart", description = "Get shipping options for cart review - typically called when user views cart")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Cart shipping options retrieved successfully", content = @Content(schema = @Schema(implementation = ShippingQuoteResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid cart data"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<ShippingQuoteResponse> reviewCartShipping(
      @Valid @RequestBody ShippingQuoteRequest request,
      @Parameter(description = "Include cached results if available") @RequestParam(defaultValue = "true") boolean allowCached,
      @AccountRequest TAccountRequest accountRequest) {

    log.info("Reviewing cart shipping for {} items",
        request.isMultiVendorRequest() ? request.getVendorPackages().size() : 1);

    // For cart review, try cached results first if allowed
    ShippingQuoteResponse response = null;
    if (allowCached) {
      response = shippingQuoteService.getCachedQuotes(request, accountRequest);
    }

    // If no cached results or cache not allowed, calculate fresh quotes
    if (response == null) {
      if (request.isSingleVendorRequest()) {
        response = shippingQuoteService.getShippingQuotes(request, accountRequest);
      } else {
        response = shippingQuoteService.getMultiVendorShippingQuotes(request, accountRequest);
      }
    }

    return ResponseEntity.ok(response);
  }

  @PostMapping("/checkout/calculate")
  @Operation(summary = "Calculate shipping costs for checkout", description = "Get final shipping costs for checkout - always fresh calculation")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Checkout shipping costs calculated successfully", content = @Content(schema = @Schema(implementation = ShippingQuoteResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid checkout data"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<ShippingQuoteResponse> calculateCheckoutShipping(
      @Valid @RequestBody ShippingQuoteRequest request,
      @Parameter(description = "Force fresh calculation (ignore cache)") @RequestParam(defaultValue = "true") boolean forceFresh,
      @AccountRequest TAccountRequest accountRequest) {

    log.info("Calculating checkout shipping costs (forceFresh: {})", forceFresh);

    // For checkout, always get fresh quotes unless explicitly allowed to use cache
    ShippingQuoteResponse response;

    if (!forceFresh) {
      response = shippingQuoteService.getCachedQuotes(request, accountRequest);
      if (response != null) {
        log.info("Using cached quotes for checkout");
        return ResponseEntity.ok(response);
      }
    }

    // Calculate fresh quotes
    if (request.isSingleVendorRequest()) {
      response = shippingQuoteService.getShippingQuotes(request, accountRequest);
    } else {
      response = shippingQuoteService.getMultiVendorShippingQuotes(request, accountRequest);
    }

    return ResponseEntity.ok(response);
  }

  @PostMapping("/quick-estimate")
  @Operation(summary = "Quick shipping estimate", description = "Get a quick shipping estimate with limited providers (faster response)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Quick estimate calculated successfully", content = @Content(schema = @Schema(implementation = ShippingQuoteResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<ShippingQuoteResponse> getQuickEstimate(
      @Valid @RequestBody ShippingQuoteRequest request,
      @Parameter(description = "Maximum number of providers to query") @RequestParam(defaultValue = "3") int maxProviders,
      @AccountRequest TAccountRequest accountRequest) {

    log.info("Getting quick shipping estimate with max {} providers", maxProviders);

    // For quick estimates, check cache first
    ShippingQuoteResponse cachedResponse = shippingQuoteService.getCachedQuotes(request, accountRequest);
    if (cachedResponse != null) {
      // Limit the number of options returned for quick estimate
      List<ShippingQuoteResponse.ShippingOption> limitedOptions = cachedResponse.getShippingOptions()
          .stream()
          .limit(maxProviders)
          .toList();

      ShippingQuoteResponse quickResponse = ShippingQuoteResponse.builder()
          .quoteId(cachedResponse.getQuoteId())
          .quotedAt(cachedResponse.getQuotedAt())
          .expiresAt(cachedResponse.getExpiresAt())
          .requestType(cachedResponse.getRequestType())
          .shippingOptions(limitedOptions)
          .recommendedOption(cachedResponse.getRecommendedOption())
          .cheapestOption(cachedResponse.getCheapestValidOption())
          .fastestOption(cachedResponse.getFastestValidOption())
          .metadata(cachedResponse.getMetadata())
          .build();

      return ResponseEntity.ok(quickResponse);
    }

    // If no cache, calculate with limited providers
    ShippingQuoteResponse response;
    if (request.isSingleVendorRequest()) {
      response = shippingQuoteService.getShippingQuotes(request, accountRequest);
    } else {
      response = shippingQuoteService.getMultiVendorShippingQuotes(request, accountRequest);
    }

    return ResponseEntity.ok(response);
  }

  @GetMapping("/providers/{providerName}")
  @Operation(summary = "Get quotes from specific provider", description = "Get shipping quotes from a specific provider only")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Provider quotes retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "Provider not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<List<ShippingQuoteResponse.ShippingOption>> getQuotesFromProvider(
      @Parameter(description = "Provider name") @PathVariable String providerName,
      @Valid @RequestBody ShippingQuoteRequest request,
      @AccountRequest TAccountRequest accountRequest) {

    log.info("Getting quotes from provider: {}", providerName);

    List<ShippingQuoteResponse.ShippingOption> options = shippingQuoteService.getQuotesFromProvider(request,
        providerName, accountRequest);

    return ResponseEntity.ok(options);
  }

  @GetMapping("/providers")
  @Operation(summary = "Get available providers", description = "Get list of available shipping providers for a route")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Available providers retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<List<String>> getAvailableProviders(
      @Parameter(description = "Origin country code") @RequestParam String originCountry,
      @Parameter(description = "Destination country code") @RequestParam String destinationCountry,
      @AccountRequest TAccountRequest accountRequest) {

    log.info("Getting available providers for route: {} -> {}", originCountry, destinationCountry);

    List<String> providers = shippingQuoteService.getAvailableProviders(originCountry, destinationCountry);

    return ResponseEntity.ok(providers);
  }

  @PostMapping("/validate")
  @Operation(summary = "Validate shipping quote request", description = "Validate a shipping quote request without calculating quotes")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Validation completed"),
      @ApiResponse(responseCode = "400", description = "Validation failed")
  })
  public ResponseEntity<ShippingQuoteService.ValidationResult> validateShippingQuoteRequest(
      @Valid @RequestBody ShippingQuoteRequest request) {

    log.info("Validating shipping quote request");

    ShippingQuoteService.ValidationResult result = shippingQuoteService.validateShippingQuoteRequest(request);

    if (result.isValid()) {
      return ResponseEntity.ok(result);
    } else {
      return ResponseEntity.badRequest().body(result);
    }
  }

  @GetMapping("/providers/status")
  @Operation(summary = "Get provider status", description = "Get status and configuration of all shipping providers")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Provider status retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<Map<String, Object>> getProviderStatus(
      @AccountRequest TAccountRequest accountRequest) {

    log.info("Getting provider status");

    Map<String, Object> status = shippingQuoteService.getProviderStatus();

    return ResponseEntity.ok(status);
  }

  @PostMapping("/providers/test-connectivity")
  @Operation(summary = "Test provider connectivity", description = "Test connectivity to all shipping providers")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Connectivity test completed"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public ResponseEntity<Map<String, Boolean>> testProviderConnectivity(
      @AccountRequest TAccountRequest accountRequest) {

    log.info("Testing provider connectivity");

    Map<String, Boolean> connectivity = shippingQuoteService.testProviderConnectivity();

    return ResponseEntity.ok(connectivity);
  }

  // Error handling
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
    log.error("Invalid argument: {}", e.getMessage());
    return ResponseEntity.badRequest()
        .body(Map.of("error", "Invalid request", "message", e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
    log.error("Unexpected error in shipping quotes", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", "Internal server error", "message", "An unexpected error occurred"));
  }
}
