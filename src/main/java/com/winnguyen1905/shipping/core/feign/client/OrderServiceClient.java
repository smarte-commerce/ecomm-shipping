package com.winnguyen1905.shipping.core.feign.client;

import com.winnguyen1905.shipping.core.feign.dto.OrderDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
    name = "order-service",
    url = "${microservices.order-service.url:http://localhost:8081}",
    fallback = OrderServiceClientFallback.class
)
@CircuitBreaker(name = "order-service")
@Retry(name = "order-service")
public interface OrderServiceClient {

    /**
     * Get order details by order ID
     */
    @GetMapping("/api/v1/orders/{orderId}")
    ResponseEntity<OrderDto> getOrderById(@PathVariable("orderId") Long orderId);

    /**
     * Get order details by order number
     */
    @GetMapping("/api/v1/orders/by-number/{orderNumber}")
    ResponseEntity<OrderDto> getOrderByNumber(@PathVariable("orderNumber") String orderNumber);

    /**
     * Get orders by customer ID
     */
    @GetMapping("/api/v1/orders/customer/{customerId}")
    ResponseEntity<List<OrderDto>> getOrdersByCustomerId(@PathVariable("customerId") Long customerId);

    /**
     * Get orders by status
     */
    @GetMapping("/api/v1/orders")
    ResponseEntity<List<OrderDto>> getOrdersByStatus(@RequestParam("status") String status);

    /**
     * Update order status
     */
    @PutMapping("/api/v1/orders/{orderId}/status")
    ResponseEntity<OrderDto> updateOrderStatus(
        @PathVariable("orderId") Long orderId,
        @RequestBody UpdateOrderStatusRequest request
    );

    /**
     * Add shipping information to order
     */
    @PutMapping("/api/v1/orders/{orderId}/shipping")
    ResponseEntity<OrderDto> updateOrderShippingInfo(
        @PathVariable("orderId") Long orderId,
        @RequestBody OrderShippingInfoRequest request
    );

    /**
     * Get order items for shipping
     */
    @GetMapping("/api/v1/orders/{orderId}/items")
    ResponseEntity<List<OrderDto.OrderItemDto>> getOrderItems(@PathVariable("orderId") Long orderId);

    /**
     * Mark order as shipped
     */
    @PostMapping("/api/v1/orders/{orderId}/ship")
    ResponseEntity<OrderDto> markOrderAsShipped(
        @PathVariable("orderId") Long orderId,
        @RequestBody ShipOrderRequest request
    );

    /**
     * Mark order as delivered
     */
    @PostMapping("/api/v1/orders/{orderId}/deliver")
    ResponseEntity<OrderDto> markOrderAsDelivered(
        @PathVariable("orderId") Long orderId,
        @RequestBody DeliverOrderRequest request
    );

    /**
     * Check if order is ready for shipping
     */
    @GetMapping("/api/v1/orders/{orderId}/shipping-ready")
    ResponseEntity<Boolean> isOrderReadyForShipping(@PathVariable("orderId") Long orderId);

    // DTOs for request/response
    record UpdateOrderStatusRequest(String status, String reason, String notes) {}
    
    record OrderShippingInfoRequest(
        String carrierId,
        String carrierName,
        String trackingNumber,
        String shippingLabelUrl,
        String estimatedDeliveryDate
    ) {}
    
    record ShipOrderRequest(
        String trackingNumber,
        String carrierId,
        String carrierName,
        String shippingMethod,
        String estimatedDeliveryDate
    ) {}
    
    record DeliverOrderRequest(
        String trackingNumber,
        String deliverySignature,
        String deliveryNotes,
        String deliveredAt
    ) {}
} 
