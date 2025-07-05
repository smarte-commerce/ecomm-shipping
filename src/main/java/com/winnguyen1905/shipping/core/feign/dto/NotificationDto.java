package com.winnguyen1905.shipping.core.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    
    private String notificationId;
    
    private String recipientId;
    
    private String recipientEmail;
    
    private String recipientPhone;
    
    private String notificationType; // EMAIL, SMS, PUSH, IN_APP
    
    private String templateId;
    
    private String subject;
    
    private String message;
    
    private String content;
    
    private Map<String, Object> templateVariables;
    
    private String priority; // LOW, NORMAL, HIGH, URGENT
    
    private String category; // SHIPPING, ORDER, DELIVERY, TRACKING
    
    private Boolean sendImmediately;
    
    private Instant scheduledAt;
    
    private Integer retryCount;
    
    private Integer maxRetries;
    
    private String status; // PENDING, SENT, DELIVERED, FAILED, CANCELLED
    
    private String errorMessage;
    
    private Map<String, Object> metadata;
    
    private Instant createdAt;
    
    private Instant sentAt;
    
    private Instant deliveredAt;
    
    // Request DTO for sending notifications
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationRequest {
        
        private String recipientId;
        
        private String recipientEmail;
        
        private String recipientPhone;
        
        private String notificationType;
        
        private String templateId;
        
        private String subject;
        
        private String message;
        
        private Map<String, Object> templateVariables;
        
        private String priority;
        
        private String category;
        
        private Boolean sendImmediately;
        
        private Instant scheduledAt;
        
        private Map<String, Object> metadata;
    }
    
    // Response DTO for notification status
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationResponse {
        
        private String notificationId;
        
        private String status;
        
        private String message;
        
        private Instant sentAt;
        
        private Instant deliveredAt;
        
        private String errorMessage;
    }
} 
