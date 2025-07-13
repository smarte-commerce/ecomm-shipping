package com.winnguyen1905.shipping.core.provider.impl;

import com.winnguyen1905.shipping.core.model.request.ShippingQuoteRequest;
import com.winnguyen1905.shipping.core.model.response.ShippingQuoteResponse;
import com.winnguyen1905.shipping.core.provider.ShippingProviderClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * VNPost (Vietnam Post) implementation of ShippingProviderClient
 * Focuses on domestic Vietnamese shipping with some international capabilities
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VNPostShippingClient implements ShippingProviderClient {

    @Value("${shipping.providers.vnpost.api-key:#{null}}")
    private String apiKey;

    @Value("${shipping.providers.vnpost.base-url:https://api.vnpost.vn/api}")
    private String baseUrl;

    @Value("${shipping.providers.vnpost.enabled:false}")
    private boolean enabled;

    @Override
    public List<ShippingQuoteResponse.ShippingOption> getShippingQuotes(ShippingQuoteRequest request) {
        if (!enabled || !isAvailable()) {
            log.warn("VNPost provider is not available");
            return Collections.emptyList();
        }

        try {
            log.info("Getting shipping quotes from VNPost for request: {}", request);
            
            // Check if route is supported
            if (!supportsRoute(request.getVendor().getAddress().getCountry(), 
                              request.getCustomer().getAddress().getCountry())) {
                log.warn("VNPost does not support route from {} to {}", 
                        request.getVendor().getAddress().getCountry(),
                        request.getCustomer().getAddress().getCountry());
                return Collections.emptyList();
            }

            // Check package limits
            if (!supportsPackage(request.getPackageInfo().getWeight(), 
                               request.getPackageInfo().getDimensions(), 
                               request.getPackageInfo().getDeclaredValue())) {
                log.warn("VNPost does not support package specifications");
                return Collections.emptyList();
            }

            // Calculate rates based on VNPost pricing model
            return calculateVNPostRates(request);
            
        } catch (Exception e) {
            log.error("Error getting quotes from VNPost", e);
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
            // Only process if both vendor and customer are in Vietnam
            if ("VN".equals(vendorPackage.getVendor().getAddress().getCountry()) ||
                "VN".equals(request.getCustomer().getAddress().getCountry())) {
                
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
        }
        
        return result;
    }

    @Override
    public boolean supportsRoute(String originCountry, String destinationCountry) {
        // VNPost primarily serves Vietnam domestic routes, with some international capabilities
        if ("VN".equals(originCountry) && "VN".equals(destinationCountry)) {
            return true; // Domestic Vietnam
        }
        
        // International routes from Vietnam
        if ("VN".equals(originCountry)) {
            List<String> internationalDestinations = List.of(
                "US", "CA", "GB", "AU", "JP", "KR", "TH", "SG", "MY", "ID", "PH", "CN", "TW", "HK"
            );
            return internationalDestinations.contains(destinationCountry);
        }
        
        return false;
    }

    @Override
    public boolean supportsPackage(BigDecimal weight, ShippingQuoteRequest.DimensionsInfo dimensions, BigDecimal declaredValue) {
        // VNPost package limits
        if (weight.compareTo(getMaxPackageWeight()) > 0) {
            return false;
        }

        // Dimension limits for VNPost
        BigDecimal maxSingleDimension = new BigDecimal("100"); // 100 cm
        BigDecimal maxTotalDimensions = new BigDecimal("200"); // L+W+H <= 200 cm
        
        if (dimensions.getLength().compareTo(maxSingleDimension) > 0 ||
            dimensions.getWidth().compareTo(maxSingleDimension) > 0 ||
            dimensions.getHeight().compareTo(maxSingleDimension) > 0) {
            return false;
        }

        BigDecimal totalDimensions = dimensions.getLength()
                .add(dimensions.getWidth())
                .add(dimensions.getHeight());
        
        if (totalDimensions.compareTo(maxTotalDimensions) > 0) {
            return false;
        }

        return declaredValue.compareTo(getMaxDeclaredValue()) <= 0;
    }

    @Override
    public String getProviderName() {
        return "VNPost";
    }

    @Override
    public List<String> getSupportedCountries() {
        return List.of("VN", "US", "CA", "GB", "AU", "JP", "KR", "TH", "SG", "MY", "ID", "PH", "CN", "TW", "HK");
    }

    @Override
    public BigDecimal getMaxPackageWeight() {
        return new BigDecimal("30"); // 30 kg limit
    }

    @Override
    public BigDecimal getMaxDeclaredValue() {
        // Convert to VND equivalent of $10,000 USD
        return new BigDecimal("240000000"); // 240M VND
    }

    @Override
    public boolean isAvailable() {
        return enabled; // VNPost might not require API key for basic rate calculation
    }

    @Override
    public Map<String, Object> getProviderConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "VNPost");
        config.put("enabled", enabled);
        config.put("baseUrl", baseUrl);
        config.put("maxWeight", getMaxPackageWeight());
        config.put("maxValue", getMaxDeclaredValue());
        config.put("primaryMarket", "Vietnam");
        return config;
    }

    // Private helper methods

    private List<ShippingQuoteResponse.ShippingOption> calculateVNPostRates(ShippingQuoteRequest request) {
        List<ShippingQuoteResponse.ShippingOption> options = new ArrayList<>();
        
        boolean isDomestic = "VN".equals(request.getVendor().getAddress().getCountry()) && 
                           "VN".equals(request.getCustomer().getAddress().getCountry());
        
        if (isDomestic) {
            options.addAll(getDomesticVNPostRates(request));
        } else {
            options.addAll(getInternationalVNPostRates(request));
        }
        
        return options;
    }

    private List<ShippingQuoteResponse.ShippingOption> getDomesticVNPostRates(ShippingQuoteRequest request) {
        List<ShippingQuoteResponse.ShippingOption> options = new ArrayList<>();
        BigDecimal weight = request.getPackageInfo().getWeight();
        
        // VNPost Express (same city)
        if (isSameCity(request)) {
            BigDecimal expressRate = calculateDomesticRate(weight, "EXPRESS");
            options.add(createVNPostOption("Express", expressRate, "VND", 1, "VNP_EXPRESS"));
        }
        
        // VNPost Standard
        BigDecimal standardRate = calculateDomesticRate(weight, "STANDARD");
        options.add(createVNPostOption("Standard", standardRate, "VND", 3, "VNP_STANDARD"));
        
        // VNPost Economy
        BigDecimal economyRate = calculateDomesticRate(weight, "ECONOMY");
        options.add(createVNPostOption("Economy", economyRate, "VND", 5, "VNP_ECONOMY"));
        
        return options;
    }

    private List<ShippingQuoteResponse.ShippingOption> getInternationalVNPostRates(ShippingQuoteRequest request) {
        List<ShippingQuoteResponse.ShippingOption> options = new ArrayList<>();
        
        // EMS International
        BigDecimal emsRate = calculateInternationalRate(request, "EMS");
        options.add(createVNPostOption("EMS International", emsRate, "VND", 7, "VNP_EMS_INTL"));
        
        // Regular International
        BigDecimal regularRate = calculateInternationalRate(request, "REGULAR");
        options.add(createVNPostOption("Regular International", regularRate, "VND", 14, "VNP_REG_INTL"));
        
        return options;
    }

    private BigDecimal calculateDomesticRate(BigDecimal weight, String serviceType) {
        BigDecimal baseRate;
        BigDecimal perKgRate;
        
        switch (serviceType) {
            case "EXPRESS":
                baseRate = new BigDecimal("25000"); // 25,000 VND base
                perKgRate = new BigDecimal("8000"); // 8,000 VND per kg
                break;
            case "STANDARD":
                baseRate = new BigDecimal("15000"); // 15,000 VND base
                perKgRate = new BigDecimal("5000"); // 5,000 VND per kg
                break;
            case "ECONOMY":
                baseRate = new BigDecimal("10000"); // 10,000 VND base
                perKgRate = new BigDecimal("3000"); // 3,000 VND per kg
                break;
            default:
                baseRate = new BigDecimal("15000");
                perKgRate = new BigDecimal("5000");
        }
        
        // Calculate total rate
        BigDecimal additionalWeight = weight.subtract(BigDecimal.ONE).max(BigDecimal.ZERO);
        return baseRate.add(additionalWeight.multiply(perKgRate));
    }

    private BigDecimal calculateInternationalRate(ShippingQuoteRequest request, String serviceType) {
        BigDecimal weight = request.getPackageInfo().getWeight();
        String destinationCountry = request.getCustomer().getAddress().getCountry();
        
        BigDecimal baseRate;
        BigDecimal perKgRate;
        BigDecimal zoneMultiplier = getZoneMultiplier(destinationCountry);
        
        switch (serviceType) {
            case "EMS":
                baseRate = new BigDecimal("200000"); // 200,000 VND base
                perKgRate = new BigDecimal("50000"); // 50,000 VND per kg
                break;
            case "REGULAR":
                baseRate = new BigDecimal("80000"); // 80,000 VND base
                perKgRate = new BigDecimal("20000"); // 20,000 VND per kg
                break;
            default:
                baseRate = new BigDecimal("80000");
                perKgRate = new BigDecimal("20000");
        }
        
        BigDecimal additionalWeight = weight.subtract(BigDecimal.ONE).max(BigDecimal.ZERO);
        BigDecimal totalRate = baseRate.add(additionalWeight.multiply(perKgRate));
        
        return totalRate.multiply(zoneMultiplier);
    }

    private BigDecimal getZoneMultiplier(String countryCode) {
        // Zone pricing for international shipping
        return switch (countryCode) {
            case "TH", "SG", "MY", "ID", "PH", "KH", "LA" -> new BigDecimal("1.0"); // ASEAN
            case "CN", "TW", "HK", "JP", "KR" -> new BigDecimal("1.2"); // East Asia
            case "AU", "NZ" -> new BigDecimal("1.5"); // Oceania
            case "US", "CA" -> new BigDecimal("1.8"); // North America
            case "GB", "DE", "FR" -> new BigDecimal("2.0"); // Europe
            default -> new BigDecimal("2.5"); // Rest of world
        };
    }

    private boolean isSameCity(ShippingQuoteRequest request) {
        String vendorCity = request.getVendor().getAddress().getCity();
        String customerCity = request.getCustomer().getAddress().getCity();
        return vendorCity != null && vendorCity.equalsIgnoreCase(customerCity);
    }

    private ShippingQuoteResponse.ShippingOption createVNPostOption(String service, BigDecimal cost, 
                                                                   String currency, int deliveryDays, 
                                                                   String serviceCode) {
        return ShippingQuoteResponse.ShippingOption.builder()
                .provider("VNPost")
                .service(service)
                .cost(cost)
                .currency(currency)
                .estimatedDays(deliveryDays)
                .estimatedDeliveryDate(LocalDate.now().plusDays(deliveryDays))
                .serviceCode(serviceCode)
                .trackingSupported("Yes")
                .insuranceIncluded(true)
                .deliveryType("DOOR_TO_DOOR")
                .features(List.of("Tracking", "Insurance included", "COD available"))
                .providerLogo("https://vnpost.vn/logo.png")
                .rating(ShippingQuoteResponse.ProviderRating.builder()
                        .overallRating(3.8)
                        .deliveryTimeRating(3.5)
                        .reliabilityRating(4.0)
                        .customerServiceRating(3.9)
                        .totalReviews(850)
                        .build())
                .build();
    }
} 
