package com.winnguyen1905.shipping.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winnguyen1905.shipping.core.model.request.ShippingQuoteRequest;
import com.winnguyen1905.shipping.core.model.response.ShippingQuoteResponse;
import com.winnguyen1905.shipping.core.provider.ShippingProviderClient;
import com.winnguyen1905.shipping.core.service.ShippingQuoteService;
import com.winnguyen1905.shipping.secure.TAccountRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingQuoteServiceImpl implements ShippingQuoteService {

    private final List<ShippingProviderClient> shippingProviders;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "shipping_quotes:";
    private static final int CACHE_TTL_MINUTES = 10;
    private static final int MAX_PROVIDERS_PARALLEL = 5;

    @Override
    @CircuitBreaker(name = "shipping-quotes")
    @Retry(name = "shipping-quotes")
    public ShippingQuoteResponse getShippingQuotes(ShippingQuoteRequest request, TAccountRequest accountRequest) {
        log.info("Getting shipping quotes for request: {}", request);
        
        // Validate request
        ValidationResult validation = validateShippingQuoteRequest(request);
        if (!validation.isValid()) {
            log.error("Invalid shipping quote request: {}", validation.errors());
            throw new IllegalArgumentException("Invalid request: " + String.join(", ", validation.errors()));
        }

        // Check cache first
        ShippingQuoteResponse cachedResponse = getCachedQuotes(request, accountRequest);
        if (cachedResponse != null) {
            log.info("Returning cached quotes for request");
            return cachedResponse;
        }

        // Get quotes from all providers
        List<ShippingQuoteResponse.ShippingOption> allOptions = new ArrayList<>();
        List<ShippingQuoteResponse.ProviderError> providerErrors = new ArrayList<>();
        List<String> availableProviders = new ArrayList<>();
        List<String> unavailableProviders = new ArrayList<>();

        // Get applicable providers for this route
        String originCountry = request.isSingleVendorRequest() ? 
            request.getVendor().getAddress().getCountry() : "MULTI";
        String destinationCountry = request.getCustomer().getAddress().getCountry();

        List<ShippingProviderClient> applicableProviders = getApplicableProviders(originCountry, destinationCountry);

        // Query providers in parallel with timeout
        List<CompletableFuture<Void>> futures = applicableProviders.stream()
            .limit(MAX_PROVIDERS_PARALLEL)
            .map(provider -> CompletableFuture.runAsync(() -> {
                try {
                    List<ShippingQuoteResponse.ShippingOption> options = getQuotesFromProviderWithFallback(provider, request);
                    synchronized (allOptions) {
                        allOptions.addAll(options);
                        availableProviders.add(provider.getProviderName());
                    }
                } catch (Exception e) {
                    log.error("Error getting quotes from provider {}: {}", provider.getProviderName(), e.getMessage());
                    synchronized (providerErrors) {
                        providerErrors.add(ShippingQuoteResponse.ProviderError.builder()
                            .provider(provider.getProviderName())
                            .errorCode("PROVIDER_ERROR")
                            .errorMessage(e.getMessage())
                            .fallbackUsed("Internal calculation")
                            .build());
                        unavailableProviders.add(provider.getProviderName());
                    }
                }
            }))
            .toList();

        // Wait for all providers to complete (with timeout)
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Timeout or error waiting for provider responses", e);
        }

        // If no external options available, use internal calculation as fallback
        if (allOptions.isEmpty()) {
            log.warn("No external provider options available, using internal fallback");
            allOptions.addAll(calculateInternalFallbackRates(request));
        }

        // Build response
        ShippingQuoteResponse response = buildShippingQuoteResponse(
            request, allOptions, availableProviders, unavailableProviders, providerErrors);

        // Cache the response
        cacheQuotes(request, response, accountRequest);

        log.info("Returning {} shipping options from {} providers", 
                allOptions.size(), availableProviders.size());
        
        return response;
    }

    @Override
    public ShippingQuoteResponse getMultiVendorShippingQuotes(ShippingQuoteRequest request, TAccountRequest accountRequest) {
        if (!request.isMultiVendorRequest()) {
            throw new IllegalArgumentException("Request must be multi-vendor");
        }

        log.info("Getting multi-vendor shipping quotes for {} vendors", request.getVendorPackages().size());

        Map<String, ShippingQuoteResponse> vendorQuotes = new HashMap<>();
        List<ShippingQuoteResponse.ProviderError> allErrors = new ArrayList<>();

        // Get quotes for each vendor separately
        for (ShippingQuoteRequest.VendorPackageInfo vendorPackage : request.getVendorPackages()) {
            ShippingQuoteRequest singleVendorRequest = ShippingQuoteRequest.builder()
                .customer(request.getCustomer())
                .vendor(vendorPackage.getVendor())
                .packageInfo(vendorPackage.getPackageInfo())
                .requireInsurance(request.getRequireInsurance())
                .preferredCurrency(request.getPreferredCurrency())
                .build();

            try {
                ShippingQuoteResponse vendorQuote = getShippingQuotes(singleVendorRequest, accountRequest);
                vendorQuotes.put(vendorPackage.getVendor().getVendorId(), vendorQuote);
                
                // Mark options with vendor info
                vendorQuote.getShippingOptions().forEach(option -> {
                    option.setVendorId(vendorPackage.getVendor().getVendorId());
                    option.setVendorName(vendorPackage.getVendor().getName());
                });
            } catch (Exception e) {
                log.error("Error getting quotes for vendor {}: {}", 
                         vendorPackage.getVendor().getVendorId(), e.getMessage());
                allErrors.add(ShippingQuoteResponse.ProviderError.builder()
                    .provider("VENDOR_" + vendorPackage.getVendor().getVendorId())
                    .errorCode("VENDOR_ERROR")
                    .errorMessage(e.getMessage())
                    .build());
            }
        }

        // Consolidate all options
        List<ShippingQuoteResponse.ShippingOption> allOptions = vendorQuotes.values()
            .stream()
            .flatMap(quote -> quote.getShippingOptions().stream())
            .toList();

        // Calculate metadata
        ShippingQuoteResponse.QuoteMetadata metadata = ShippingQuoteResponse.QuoteMetadata.builder()
            .originCountry("MULTI")
            .destinationCountry(request.getCustomer().getAddress().getCountry())
            .isDomestic(false)
            .requiresCustoms(true)
            .totalPackages(request.getVendorPackages().size())
            .totalWeight(request.getVendorPackages().stream()
                .map(vp -> vp.getPackageInfo().getWeight())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .totalDeclaredValue(request.getVendorPackages().stream()
                .map(vp -> vp.getPackageInfo().getDeclaredValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .baseCurrency(request.getPreferredCurrency() != null ? request.getPreferredCurrency() : "USD")
            .availableProviders(vendorQuotes.values().stream()
                .flatMap(q -> q.getMetadata().getAvailableProviders().stream())
                .distinct()
                .toList())
            .providerErrors(allErrors)
            .calculationMethod("HYBRID")
            .build();

        return ShippingQuoteResponse.builder()
            .quoteId(generateQuoteId())
            .quotedAt(Instant.now())
            .expiresAt(Instant.now().plus(CACHE_TTL_MINUTES, ChronoUnit.MINUTES))
            .requestType("MULTI_VENDOR")
            .shippingOptions(allOptions)
            .recommendedOption(findBestOption(allOptions, "BALANCED"))
            .cheapestOption(findBestOption(allOptions, "CHEAPEST"))
            .fastestOption(findBestOption(allOptions, "FASTEST"))
            .metadata(metadata)
            .build();
    }

    @Override
    @Cacheable(value = "shipping_quotes", key = "#request.hashCode()")
    public ShippingQuoteResponse getCachedQuotes(ShippingQuoteRequest request, TAccountRequest accountRequest) {
        try {
            String cacheKey = CACHE_PREFIX + generateCacheKey(request);
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                log.debug("Found cached quotes for key: {}", cacheKey);
                return objectMapper.convertValue(cached, ShippingQuoteResponse.class);
            }
        } catch (Exception e) {
            log.warn("Error retrieving cached quotes: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public void cacheQuotes(ShippingQuoteRequest request, ShippingQuoteResponse response, TAccountRequest accountRequest) {
        try {
            String cacheKey = CACHE_PREFIX + generateCacheKey(request);
            redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("Cached quotes for key: {}", cacheKey);
        } catch (Exception e) {
            log.warn("Error caching quotes: {}", e.getMessage());
        }
    }

    @Override
    public List<ShippingQuoteResponse.ShippingOption> getQuotesFromProvider(
            ShippingQuoteRequest request, String providerName, TAccountRequest accountRequest) {
        
        Optional<ShippingProviderClient> provider = shippingProviders.stream()
            .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
            .findFirst();

        if (provider.isEmpty()) {
            throw new IllegalArgumentException("Provider not found: " + providerName);
        }

        return getQuotesFromProviderWithFallback(provider.get(), request);
    }

    @Override
    public List<String> getAvailableProviders(String originCountry, String destinationCountry) {
        return shippingProviders.stream()
            .filter(ShippingProviderClient::isAvailable)
            .filter(provider -> provider.supportsRoute(originCountry, destinationCountry))
            .map(ShippingProviderClient::getProviderName)
            .toList();
    }

    @Override
    public ValidationResult validateShippingQuoteRequest(ShippingQuoteRequest request) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (request == null) {
            errors.add("Request cannot be null");
            return new ValidationResult(false, errors, warnings);
        }

        if (!request.isValidRequest()) {
            errors.add("Request must be either single vendor or multi vendor, not both");
        }

        if (request.getCustomer() == null) {
            errors.add("Customer information is required");
        } else {
            if (request.getCustomer().getAddress() == null) {
                errors.add("Customer address is required");
            }
        }

        if (request.isSingleVendorRequest()) {
            if (request.getVendor() == null) {
                errors.add("Vendor information is required for single vendor request");
            }
            if (request.getPackageInfo() == null) {
                errors.add("Package information is required for single vendor request");
            }
        }

        if (request.isMultiVendorRequest()) {
            if (request.getVendorPackages() == null || request.getVendorPackages().isEmpty()) {
                errors.add("Vendor packages are required for multi vendor request");
            }
        }

        // Add warnings for potential issues
        if (request.isSingleVendorRequest() && request.getPackageInfo() != null) {
            BigDecimal weight = request.getPackageInfo().getWeight();
            if (weight.compareTo(new BigDecimal("50")) > 0) {
                warnings.add("Package weight exceeds 50kg - limited carrier options may be available");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    public Map<String, Object> getProviderStatus() {
        Map<String, Object> status = new HashMap<>();
        
        for (ShippingProviderClient provider : shippingProviders) {
            Map<String, Object> providerStatus = new HashMap<>();
            providerStatus.put("available", provider.isAvailable());
            providerStatus.put("configuration", provider.getProviderConfiguration());
            providerStatus.put("supportedCountries", provider.getSupportedCountries());
            providerStatus.put("maxWeight", provider.getMaxPackageWeight());
            providerStatus.put("maxValue", provider.getMaxDeclaredValue());
            
            status.put(provider.getProviderName(), providerStatus);
        }
        
        return status;
    }

    @Override
    public Map<String, Boolean> testProviderConnectivity() {
        Map<String, Boolean> connectivity = new HashMap<>();
        
        for (ShippingProviderClient provider : shippingProviders) {
            try {
                // Simple connectivity test
                boolean isConnected = provider.isAvailable() && 
                    !provider.getSupportedCountries().isEmpty();
                connectivity.put(provider.getProviderName(), isConnected);
            } catch (Exception e) {
                log.error("Error testing connectivity for provider {}: {}", 
                         provider.getProviderName(), e.getMessage());
                connectivity.put(provider.getProviderName(), false);
            }
        }
        
        return connectivity;
    }

    // Private helper methods

    private List<ShippingProviderClient> getApplicableProviders(String originCountry, String destinationCountry) {
        return shippingProviders.stream()
            .filter(ShippingProviderClient::isAvailable)
            .filter(provider -> {
                if ("MULTI".equals(originCountry)) {
                    // For multi-vendor, check if provider supports the destination
                    return provider.getSupportedCountries().contains(destinationCountry);
                }
                return provider.supportsRoute(originCountry, destinationCountry);
            })
            .toList();
    }

    private List<ShippingQuoteResponse.ShippingOption> getQuotesFromProviderWithFallback(
            ShippingProviderClient provider, ShippingQuoteRequest request) {
        try {
            if (request.isSingleVendorRequest()) {
                return provider.getShippingQuotes(request);
            } else {
                Map<String, List<ShippingQuoteResponse.ShippingOption>> multiVendorQuotes = 
                    provider.getMultiVendorQuotes(request);
                return multiVendorQuotes.values().stream()
                    .flatMap(List::stream)
                    .toList();
            }
        } catch (Exception e) {
            log.error("Error getting quotes from provider {}, using fallback", provider.getProviderName(), e);
            return Collections.emptyList();
        }
    }

    private List<ShippingQuoteResponse.ShippingOption> calculateInternalFallbackRates(ShippingQuoteRequest request) {
        log.info("Calculating internal fallback rates");
        
        List<ShippingQuoteResponse.ShippingOption> fallbackOptions = new ArrayList<>();
        
        // Simple internal calculation based on weight and distance
        BigDecimal weight = request.isSingleVendorRequest() ? 
            request.getPackageInfo().getWeight() : 
            request.getVendorPackages().stream()
                .map(vp -> vp.getPackageInfo().getWeight())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String originCountry = request.isSingleVendorRequest() ? 
            request.getVendor().getAddress().getCountry() : "MULTI";
        String destinationCountry = request.getCustomer().getAddress().getCountry();

        boolean isDomestic = originCountry.equals(destinationCountry);
        
        // Standard delivery
        BigDecimal standardRate = calculateStandardRate(weight, isDomestic);
        fallbackOptions.add(createFallbackOption("Standard", standardRate, 
            isDomestic ? 3 : 7, "INTERNAL_STANDARD"));
        
        // Express delivery
        BigDecimal expressRate = standardRate.multiply(new BigDecimal("1.5"));
        fallbackOptions.add(createFallbackOption("Express", expressRate, 
            isDomestic ? 1 : 3, "INTERNAL_EXPRESS"));
        
        return fallbackOptions;
    }

    private BigDecimal calculateStandardRate(BigDecimal weight, boolean isDomestic) {
        BigDecimal baseRate = isDomestic ? new BigDecimal("50000") : new BigDecimal("200000"); // VND
        BigDecimal perKgRate = isDomestic ? new BigDecimal("10000") : new BigDecimal("50000"); // VND
        
        return baseRate.add(weight.multiply(perKgRate));
    }

    private ShippingQuoteResponse.ShippingOption createFallbackOption(String service, BigDecimal cost, 
                                                                     int deliveryDays, String serviceCode) {
        return ShippingQuoteResponse.ShippingOption.builder()
            .provider("Internal")
            .service(service)
            .cost(cost)
            .currency("VND")
            .estimatedDays(deliveryDays)
            .estimatedDeliveryDate(java.time.LocalDate.now().plusDays(deliveryDays))
            .serviceCode(serviceCode)
            .trackingSupported("Limited")
            .insuranceIncluded(true)
            .deliveryType("DOOR_TO_DOOR")
            .features(List.of("Basic tracking", "Standard insurance"))
            .rating(ShippingQuoteResponse.ProviderRating.builder()
                .overallRating(3.5)
                .deliveryTimeRating(3.0)
                .reliabilityRating(4.0)
                .customerServiceRating(3.5)
                .totalReviews(100)
                .build())
            .build();
    }

    private ShippingQuoteResponse buildShippingQuoteResponse(
            ShippingQuoteRequest request,
            List<ShippingQuoteResponse.ShippingOption> allOptions,
            List<String> availableProviders,
            List<String> unavailableProviders,
            List<ShippingQuoteResponse.ProviderError> providerErrors) {

        // Sort options by cost for better presentation
        allOptions.sort(Comparator.comparing(ShippingQuoteResponse.ShippingOption::getCost));

        String originCountry = request.isSingleVendorRequest() ? 
            request.getVendor().getAddress().getCountry() : "MULTI";
        String destinationCountry = request.getCustomer().getAddress().getCountry();

        ShippingQuoteResponse.QuoteMetadata metadata = ShippingQuoteResponse.QuoteMetadata.builder()
            .originCountry(originCountry)
            .destinationCountry(destinationCountry)
            .isDomestic(originCountry.equals(destinationCountry))
            .requiresCustoms(!originCountry.equals(destinationCountry))
            .totalPackages(request.isSingleVendorRequest() ? 1 : request.getVendorPackages().size())
            .totalWeight(request.isSingleVendorRequest() ? 
                request.getPackageInfo().getWeight() :
                request.getVendorPackages().stream()
                    .map(vp -> vp.getPackageInfo().getWeight())
                    .reduce(BigDecimal.ZERO, BigDecimal::add))
            .totalDeclaredValue(request.isSingleVendorRequest() ? 
                request.getPackageInfo().getDeclaredValue() :
                request.getVendorPackages().stream()
                    .map(vp -> vp.getPackageInfo().getDeclaredValue())
                    .reduce(BigDecimal.ZERO, BigDecimal::add))
            .baseCurrency(request.getPreferredCurrency() != null ? request.getPreferredCurrency() : "VND")
            .availableProviders(availableProviders)
            .unavailableProviders(unavailableProviders)
            .providerErrors(providerErrors)
            .calculationMethod(availableProviders.isEmpty() ? "INTERNAL_CALCULATION" : "EXTERNAL_API")
            .build();

        return ShippingQuoteResponse.builder()
            .quoteId(generateQuoteId())
            .quotedAt(Instant.now())
            .expiresAt(Instant.now().plus(CACHE_TTL_MINUTES, ChronoUnit.MINUTES))
            .requestType(request.isSingleVendorRequest() ? "SINGLE_VENDOR" : "MULTI_VENDOR")
            .shippingOptions(allOptions)
            .recommendedOption(findBestOption(allOptions, "BALANCED"))
            .cheapestOption(findBestOption(allOptions, "CHEAPEST"))
            .fastestOption(findBestOption(allOptions, "FASTEST"))
            .metadata(metadata)
            .build();
    }

    private ShippingQuoteResponse.ShippingOption findBestOption(
            List<ShippingQuoteResponse.ShippingOption> options, String criteria) {
        if (options.isEmpty()) {
            return null;
        }

        return switch (criteria) {
            case "CHEAPEST" -> options.stream()
                .min(Comparator.comparing(ShippingQuoteResponse.ShippingOption::getCost))
                .orElse(null);
            case "FASTEST" -> options.stream()
                .min(Comparator.comparing(ShippingQuoteResponse.ShippingOption::getEstimatedDays))
                .orElse(null);
            case "BALANCED" -> options.stream()
                .min(Comparator.comparing(option -> {
                    // Simple scoring: normalize cost and delivery time, then combine
                    double costScore = option.getCost().doubleValue() / 100000.0; // Normalize cost
                    double timeScore = option.getEstimatedDays() * 1.0; // Time weight
                    return costScore + timeScore;
                }))
                .orElse(null);
            default -> options.get(0);
        };
    }

    private String generateQuoteId() {
        return "SQ_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateCacheKey(ShippingQuoteRequest request) {
        try {
            // Create a simplified version for caching
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("customer", request.getCustomer().getAddress());
            
            if (request.isSingleVendorRequest()) {
                cacheData.put("vendor", request.getVendor().getAddress());
                cacheData.put("package", request.getPackageInfo());
            } else {
                cacheData.put("vendors", request.getVendorPackages().stream()
                    .map(vp -> Map.of("vendor", vp.getVendor().getAddress(), "package", vp.getPackageInfo()))
                    .toList());
            }
            
            return Integer.toString(cacheData.hashCode());
        } catch (Exception e) {
            log.error("Error generating cache key", e);
            return UUID.randomUUID().toString();
        }
    }
} 
