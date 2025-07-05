package com.winnguyen1905.shipping.core.feign.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("product_id")
    private Long productId;
    
    @JsonProperty("product_name")
    private String productName;
    
    @JsonProperty("product_sku")
    private String productSku;
    
    @JsonProperty("product_code")
    private String productCode;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("subcategory")
    private String subcategory;
    
    @JsonProperty("brand")
    private String brand;
    
    @JsonProperty("weight")
    private BigDecimal weight;
    
    @JsonProperty("dimensions")
    private DimensionsDto dimensions;
    
    @JsonProperty("is_fragile")
    private Boolean isFragile;
    
    @JsonProperty("is_liquid")
    private Boolean isLiquid;
    
    @JsonProperty("is_hazardous")
    private Boolean isHazardous;
    
    @JsonProperty("requires_special_handling")
    private Boolean requiresSpecialHandling;
    
    @JsonProperty("shipping_class")
    private String shippingClass;
    
    @JsonProperty("customs_info")
    private CustomsInfoDto customsInfo;
    
    @JsonProperty("inventory_info")
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
        
        @JsonProperty("volume")
        private BigDecimal volume;
        
        @JsonProperty("dimensional_weight")
        private BigDecimal dimensionalWeight;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomsInfoDto {
        
        @JsonProperty("hs_code")
        private String hsCode;
        
        @JsonProperty("country_of_origin")
        private String countryOfOrigin;
        
        @JsonProperty("customs_value")
        private BigDecimal customsValue;
        
        @JsonProperty("customs_description")
        private String customsDescription;
        
        @JsonProperty("is_restricted")
        private Boolean isRestricted;
        
        @JsonProperty("restricted_countries")
        private List<String> restrictedCountries;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryInfoDto {
        
        @JsonProperty("available_quantity")
        private Integer availableQuantity;
        
        @JsonProperty("reserved_quantity")
        private Integer reservedQuantity;
        
        @JsonProperty("warehouse_location")
        private String warehouseLocation;
        
        @JsonProperty("is_in_stock")
        private Boolean isInStock;
        
        @JsonProperty("stock_status")
        private String stockStatus;
    }
} 
