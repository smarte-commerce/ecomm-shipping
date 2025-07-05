package com.winnguyen1905.shipping.core.feign.client;

import com.winnguyen1905.shipping.core.feign.dto.NotificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public ResponseEntity<NotificationDto.NotificationResponse> sendNotification(NotificationDto.NotificationRequest request) {
        log.warn("Notification service is unavailable. Using fallback for sendNotification");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<NotificationDto.NotificationResponse> sendShippingNotification(ShippingNotificationRequest request) {
        log.warn("Notification service is unavailable. Using fallback for sendShippingNotification");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<NotificationDto.NotificationResponse> sendDeliveryNotification(DeliveryNotificationRequest request) {
        log.warn("Notification service is unavailable. Using fallback for sendDeliveryNotification");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<NotificationDto.NotificationResponse> sendTrackingUpdateNotification(TrackingNotificationRequest request) {
        log.warn("Notification service is unavailable. Using fallback for sendTrackingUpdateNotification");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<NotificationDto.NotificationResponse> sendExceptionNotification(ExceptionNotificationRequest request) {
        log.warn("Notification service is unavailable. Using fallback for sendExceptionNotification");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<List<NotificationDto.NotificationResponse>> sendBulkNotifications(List<NotificationDto.NotificationRequest> requests) {
        log.warn("Notification service is unavailable. Using fallback for sendBulkNotifications");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<NotificationDto.NotificationResponse> getNotificationStatus(String notificationId) {
        log.warn("Notification service is unavailable. Using fallback for getNotificationStatus: {}", notificationId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<List<NotificationTemplate>> getNotificationTemplates(String category) {
        log.warn("Notification service is unavailable. Using fallback for getNotificationTemplates: {}", category);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }
} 
