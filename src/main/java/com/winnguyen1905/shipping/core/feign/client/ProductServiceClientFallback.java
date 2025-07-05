package com.winnguyen1905.shipping.core.feign.client;

import com.winnguyen1905.shipping.core.feign.dto.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ProductServiceClientFallback implements ProductServiceClient {

    @Override
    public ResponseEntity<ProductDto> getProductById(Long productId) {
        log.warn("Product service is unavailable. Using fallback for getProductById: {}", productId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<ProductDto> getProductBySku(String sku) {
        log.warn("Product service is unavailable. Using fallback for getProductBySku: {}", sku);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<List<ProductDto>> getProductsByIds(List<Long> productIds) {
        log.warn("Product service is unavailable. Using fallback for getProductsByIds: {}", productIds);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<List<ProductDto>> getProductsBySkus(List<String> skus) {
        log.warn("Product service is unavailable. Using fallback for getProductsBySkus: {}", skus);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<ProductShippingInfo> getProductShippingInfo(Long productId) {
        log.warn("Product service is unavailable. Using fallback for getProductShippingInfo: {}", productId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<List<ProductDto>> getProductsByCategory(String category) {
        log.warn("Product service is unavailable. Using fallback for getProductsByCategory: {}", category);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<List<SpecialHandlingCheck>> checkSpecialHandling(List<Long> productIds) {
        log.warn("Product service is unavailable. Using fallback for checkSpecialHandling: {}", productIds);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<ProductDto.CustomsInfoDto> getProductCustomsInfo(Long productId) {
        log.warn("Product service is unavailable. Using fallback for getProductCustomsInfo: {}", productId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<List<DimensionValidationResult>> validateProductDimensions(List<Long> productIds) {
        log.warn("Product service is unavailable. Using fallback for validateProductDimensions: {}", productIds);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }
} 
