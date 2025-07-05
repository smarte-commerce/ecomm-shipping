package com.winnguyen1905.shipping.core.feign.client;

import com.winnguyen1905.shipping.core.feign.dto.NotificationDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
    name = "notification-service",
    url = "${microservices.notification-service.url:http://localhost:8084}",
    fallback = NotificationServiceClientFallback.class
)
@CircuitBreaker(name = "notification-service")
@Retry(name = "notification-service")
public interface NotificationServiceClient {

    /**
     * Send a notification
     */
    @PostMapping("/api/v1/notifications/send")
    ResponseEntity<NotificationDto.NotificationResponse> sendNotification(@RequestBody NotificationDto.NotificationRequest request);

    /**
     * Send shipping notification
     */
    @PostMapping("/api/v1/notifications/shipping/send")
    ResponseEntity<NotificationDto.NotificationResponse> sendShippingNotification(@RequestBody ShippingNotificationRequest request);

    /**
     * Send delivery notification
     */
    @PostMapping("/api/v1/notifications/delivery/send")
    ResponseEntity<NotificationDto.NotificationResponse> sendDeliveryNotification(@RequestBody DeliveryNotificationRequest request);

    /**
     * Send tracking update notification
     */
    @PostMapping("/api/v1/notifications/tracking/send")
    ResponseEntity<NotificationDto.NotificationResponse> sendTrackingUpdateNotification(@RequestBody TrackingNotificationRequest request);

    /**
     * Send exception notification
     */
    @PostMapping("/api/v1/notifications/exception/send")
    ResponseEntity<NotificationDto.NotificationResponse> sendExceptionNotification(@RequestBody ExceptionNotificationRequest request);

    /**
     * Send bulk notifications
     */
    @PostMapping("/api/v1/notifications/bulk/send")
    ResponseEntity<List<NotificationDto.NotificationResponse>> sendBulkNotifications(@RequestBody List<NotificationDto.NotificationRequest> requests);

    /**
     * Get notification status
     */
    @GetMapping("/api/v1/notifications/{notificationId}/status")
    ResponseEntity<NotificationDto.NotificationResponse> getNotificationStatus(@PathVariable("notificationId") String notificationId);

    /**
     * Get notification templates
     */
    @GetMapping("/api/v1/notifications/templates")
    ResponseEntity<List<NotificationTemplate>> getNotificationTemplates(@RequestParam("category") String category);

    // DTOs for requests
    record ShippingNotificationRequest(
        String recipientId,
        String recipientEmail,
        String recipientPhone,
        String notificationType,
        String orderNumber,
        String trackingNumber,
        String carrierName,
        String estimatedDeliveryDate,
        java.util.Map<String, Object> additionalData
    ) {}

    record DeliveryNotificationRequest(
        String recipientId,
        String recipientEmail,
        String recipientPhone,
        String notificationType,
        String orderNumber,
        String trackingNumber,
        String deliveryStatus,
        String deliveryDate,
        String deliverySignature,
        java.util.Map<String, Object> additionalData
    ) {}

    record TrackingNotificationRequest(
        String recipientId,
        String recipientEmail,
        String recipientPhone,
        String notificationType,
        String trackingNumber,
        String eventType,
        String eventDescription,
        String eventLocation,
        String eventTimestamp,
        java.util.Map<String, Object> additionalData
    ) {}

    record ExceptionNotificationRequest(
        String recipientId,
        String recipientEmail,
        String recipientPhone,
        String notificationType,
        String trackingNumber,
        String exceptionType,
        String exceptionDescription,
        String resolutionInstructions,
        java.util.Map<String, Object> additionalData
    ) {}

    record NotificationTemplate(
        String templateId,
        String templateName,
        String category,
        String notificationType,
        String subject,
        String content,
        java.util.List<String> requiredVariables
    ) {}
} 
