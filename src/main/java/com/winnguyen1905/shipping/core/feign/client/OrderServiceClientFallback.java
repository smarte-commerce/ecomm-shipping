package com.winnguyen1905.shipping.core.feign.client;

import com.winnguyen1905.shipping.core.feign.dto.OrderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class OrderServiceClientFallback implements OrderServiceClient {

    @Override
    public ResponseEntity<OrderDto> getOrderById(Long orderId) {
        log.warn("Order service is unavailable. Using fallback for getOrderById: {}", orderId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<OrderDto> getOrderByNumber(String orderNumber) {
        log.warn("Order service is unavailable. Using fallback for getOrderByNumber: {}", orderNumber);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<List<OrderDto>> getOrdersByCustomerId(Long customerId) {
        log.warn("Order service is unavailable. Using fallback for getOrdersByCustomerId: {}", customerId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<List<OrderDto>> getOrdersByStatus(String status) {
        log.warn("Order service is unavailable. Using fallback for getOrdersByStatus: {}", status);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<OrderDto> updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        log.warn("Order service is unavailable. Using fallback for updateOrderStatus: {}", orderId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<OrderDto> updateOrderShippingInfo(Long orderId, OrderShippingInfoRequest request) {
        log.warn("Order service is unavailable. Using fallback for updateOrderShippingInfo: {}", orderId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<List<OrderDto.OrderItemDto>> getOrderItems(Long orderId) {
        log.warn("Order service is unavailable. Using fallback for getOrderItems: {}", orderId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<OrderDto> markOrderAsShipped(Long orderId, ShipOrderRequest request) {
        log.warn("Order service is unavailable. Using fallback for markOrderAsShipped: {}", orderId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<OrderDto> markOrderAsDelivered(Long orderId, DeliverOrderRequest request) {
        log.warn("Order service is unavailable. Using fallback for markOrderAsDelivered: {}", orderId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<Boolean> isOrderReadyForShipping(Long orderId) {
        log.warn("Order service is unavailable. Using fallback for isOrderReadyForShipping: {}", orderId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(false); // Default to false for safety
    }
} 
