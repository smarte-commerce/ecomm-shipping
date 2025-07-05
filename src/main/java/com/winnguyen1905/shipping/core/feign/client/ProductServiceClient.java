package com.winnguyen1905.shipping.core.feign.client;

import com.winnguyen1905.shipping.core.feign.dto.ProductDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
    name = "product-service",
    url = "${microservices.product-service.url:http://localhost:8082}",
    fallback = ProductServiceClientFallback.class
)
@CircuitBreaker(name = "product-service")
@Retry(name = "product-service")
public interface ProductServiceClient {

    /**
     * Get product details by product ID
     */
    @GetMapping("/api/v1/products/{productId}")
    ResponseEntity<ProductDto> getProductById(@PathVariable("productId") Long productId);

    /**
     * Get product details by SKU
     */
    @GetMapping("/api/v1/products/by-sku/{sku}")
    ResponseEntity<ProductDto> getProductBySku(@PathVariable("sku") String sku);

    /**
     * Get multiple products by IDs
     */
    @PostMapping("/api/v1/products/batch")
    ResponseEntity<List<ProductDto>> getProductsByIds(@RequestBody List<Long> productIds);

    /**
     * Get multiple products by SKUs
     */
    @PostMapping("/api/v1/products/batch/by-sku")
    ResponseEntity<List<ProductDto>> getProductsBySkus(@RequestBody List<String> skus);

    /**
     * Get product shipping information
     */
    @GetMapping("/api/v1/products/{productId}/shipping-info")
    ResponseEntity<ProductShippingInfo> getProductShippingInfo(@PathVariable("productId") Long productId);

    /**
     * Get products by category
     */
    @GetMapping("/api/v1/products/category/{category}")
    ResponseEntity<List<ProductDto>> getProductsByCategory(@PathVariable("category") String category);

    /**
     * Check if products require special handling
     */
    @PostMapping("/api/v1/products/special-handling/check")
    ResponseEntity<List<SpecialHandlingCheck>> checkSpecialHandling(@RequestBody List<Long> productIds);

    /**
     * Get product customs information
     */
    @GetMapping("/api/v1/products/{productId}/customs")
    ResponseEntity<ProductDto.CustomsInfoDto> getProductCustomsInfo(@PathVariable("productId") Long productId);

    /**
     * Validate product dimensions for shipping
     */
    @PostMapping("/api/v1/products/validate-dimensions")
    ResponseEntity<List<DimensionValidationResult>> validateProductDimensions(@RequestBody List<Long> productIds);

    // DTOs for responses
    record ProductShippingInfo(
        Long productId,
        String productSku,
        java.math.BigDecimal weight,
        ProductDto.DimensionsDto dimensions,
        String shippingClass,
        Boolean isFragile,
        Boolean isLiquid,
        Boolean isHazardous,
        Boolean requiresSpecialHandling,
        List<String> shippingRestrictions
    ) {}

    record SpecialHandlingCheck(
        Long productId,
        String productSku,
        Boolean requiresSpecialHandling,
        List<String> handlingRequirements,
        List<String> restrictions
    ) {}

    record DimensionValidationResult(
        Long productId,
        String productSku,
        Boolean isValid,
        List<String> validationErrors,
        java.math.BigDecimal calculatedVolume,
        java.math.BigDecimal dimensionalWeight
    ) {}
} 
