package com.winnguyen1905.shipping.core.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    
    private Long productId;
    
    private String productName;
    
    private String productSku;
    
    private String productCode;
    
    private String category;
    
    private String subcategory;
    
    private String brand;
    
    private BigDecimal weight;
    
    private DimensionsDto dimensions;
    
    private Boolean isFragile;
    
    private Boolean isLiquid;
    
    private Boolean isHazardous;
    
    private Boolean requiresSpecialHandling;
    
    private String shippingClass;
    
    private CustomsInfoDto customsInfo;
    
    private InventoryInfoDto inventoryInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionsDto {
        private BigDecimal length;
        
        private BigDecimal width;
        
        private BigDecimal height;
        
        private String unit;
        
        private BigDecimal volume;
        
        private BigDecimal dimensionalWeight;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomsInfoDto {
        
        private String hsCode;
        
        private String countryOfOrigin;
        
        private BigDecimal customsValue;
        
        private String customsDescription;
        
        private Boolean isRestricted;
        
        private List<String> restrictedCountries;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryInfoDto {
        
        private Integer availableQuantity;
        
        private Integer reservedQuantity;
        
        private String warehouseLocation;
        
        private Boolean isInStock;
        
        private String stockStatus;
    }
} 
