package com.winnguyen1905.shipping.core.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    
    private Long orderId;
    
    private Long customerId;
    
    private String orderNumber;
    
    private String orderStatus;
    
    private BigDecimal totalAmount;
    
    private String currency;
    
    private Instant orderDate;
    
    private AddressDto billingAddress;
    
    private AddressDto shippingAddress;
    
    private List<OrderItemDto> orderItems;
    
    private String paymentStatus;
    
    private String shippingPreference;
    
    private String specialInstructions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto {
        
        private Long itemId;
        
        private Long productId;
        
        private String productSku;
        
        private String productName;
        
        private Integer quantity;
        
        private BigDecimal unitPrice;
        
        private BigDecimal totalPrice;
        
        private BigDecimal weight;
        
        private DimensionsDto dimensions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        
        private String addressLine1;
        
        private String addressLine2;
        
        private String city;
        
        private String state;
        
        private String postalCode;
        
        private String country;
        
        private String companyName;
        
        private String contactName;
        
        private String phoneNumber;
        
        private String email;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionsDto {
        private BigDecimal length;
        
        private BigDecimal width;
        
        private BigDecimal height;
        
        private String unit;
    }
} 
