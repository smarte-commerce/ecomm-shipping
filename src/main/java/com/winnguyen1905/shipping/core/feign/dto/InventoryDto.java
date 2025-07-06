package com.winnguyen1905.shipping.core.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDto {
    
    private Long inventoryId;
    
    private Long productId;
    
    private String productSku;
    
    private Long warehouseId;
    
    private String warehouseCode;
    
    private String warehouseLocation;
    
    private Integer availableQuantity;
    
    private Integer reservedQuantity;
    
    private Integer totalQuantity;
    
    private Integer reorderLevel;
    
    private Integer reorderQuantity;
    
    private Boolean isInStock;
    
    private String stockStatus; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK, DISCONTINUED
    
    private Instant lastUpdated;
    
    // Request DTO for reserving inventory
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationRequest {
        
        private Long orderId;
        
        private Long customerId;
        
        private List<ReservationItem> items;
        
        private Integer reservationTimeoutMinutes;
        
        private String notes;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ReservationItem {
            
            private Long productId;
            
            private String productSku;
            
            private Integer quantity;
            
            private Long warehouseId;
        }
    }
    
    // Response DTO for inventory reservation
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationResponse {
        
        private String reservationId;
        
        private Long orderId;
        
        private String reservationStatus; // CONFIRMED, PARTIAL, FAILED
        
        private List<ReservedItem> reservedItems;
        
        private List<UnavailableItem> unavailableItems;
        
        private Instant expiresAt;
        
        private String message;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ReservedItem {
            
            private Long productId;
            
            private String productSku;
            
            private Integer requestedQuantity;
            
            private Integer reservedQuantity;
            
            private Long warehouseId;
            
            private String warehouseLocation;
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class UnavailableItem {
            
            private Long productId;
            
            private String productSku;
            
            private Integer requestedQuantity;
            
            private Integer availableQuantity;
            
            private String reason;
        }
    }
    
    // Request DTO for checking availability
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityRequest {
        
        private List<AvailabilityItem> items;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AvailabilityItem {
            
            private Long productId;
            
            private String productSku;
            
            private Integer quantity;
            
            private Long warehouseId;
        }
    }
    
    // Response DTO for availability check
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityResponse {
        
        private Boolean allAvailable;
        
        private List<ItemAvailability> itemAvailability;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ItemAvailability {
            
            private Long productId;
            
            private String productSku;
            
            private Integer requestedQuantity;
            
            private Integer availableQuantity;
            
            private Boolean isAvailable;
            
            private Long warehouseId;
            
            private Instant estimatedRestockDate;
        }
    }
} 
