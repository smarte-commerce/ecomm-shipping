package com.winnguyen1905.shipping.core.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    
    private String paymentId;
    
    private Long orderId;
    
    private Long customerId;
    
    private String paymentMethod; // CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER
    
    private String paymentType; // ORDER_PAYMENT, SHIPPING_FEE, REFUND
    
    private BigDecimal amount;
    
    private String currency;
    
    private String paymentStatus; // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED
    
    private String transactionId;
    
    private String gatewayResponse;
    
    private Instant paymentDate;
    
    private String description;
    
    private Object metadata;
    
    // Request DTO for processing payments
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRequest {
        
        private Long orderId;
        
        private Long customerId;
        
        private String paymentMethod;
        
        private String paymentType;
        
        private BigDecimal amount;
        
        private String currency;
        
        private String description;
        
        private String returnUrl;
        
        private String cancelUrl;
        
        private Object metadata;
    }
    
    // Response DTO for payment processing
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        
        private String paymentId;
        
        private String paymentStatus;
        
        private String transactionId;
        
        private String paymentUrl;
        
        private BigDecimal amount;
        
        private String currency;
        
        private Instant paymentDate;
        
        private String message;
        
        private String errorCode;
        
        private String errorMessage;
    }
    
    // DTO for refund requests
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRequest {
        
        private String paymentId;
        
        private BigDecimal refundAmount;
        
        private String reason;
        
        private String description;
        
        private Boolean notifyCustomer;
    }
    
    // DTO for refund response
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundResponse {
        
        private String refundId;
        
        private String paymentId;
        
        private String refundStatus;
        
        private BigDecimal refundAmount;
        
        private String currency;
        
        private Instant refundDate;
        
        private String message;
        
        private String errorMessage;
    }
} 
