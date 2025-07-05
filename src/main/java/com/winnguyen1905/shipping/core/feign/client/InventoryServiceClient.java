package com.winnguyen1905.shipping.core.feign.client;

import com.winnguyen1905.shipping.core.feign.dto.InventoryDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
    name = "inventory-service",
    url = "${microservices.inventory-service.url:http://localhost:8085}",
    fallback = InventoryServiceClientFallback.class
)
@CircuitBreaker(name = "inventory-service")
@Retry(name = "inventory-service")
public interface InventoryServiceClient {

    /**
     * Check product availability
     */
    @PostMapping("/api/v1/inventory/availability/check")
    ResponseEntity<InventoryDto.AvailabilityResponse> checkAvailability(@RequestBody InventoryDto.AvailabilityRequest request);

    /**
     * Reserve inventory for order
     */
    @PostMapping("/api/v1/inventory/reserve")
    ResponseEntity<InventoryDto.ReservationResponse> reserveInventory(@RequestBody InventoryDto.ReservationRequest request);

    /**
     * Release inventory reservation
     */
    @PostMapping("/api/v1/inventory/reserve/{reservationId}/release")
    ResponseEntity<Void> releaseInventoryReservation(@PathVariable("reservationId") String reservationId);

    /**
     * Confirm inventory reservation (commit)
     */
    @PostMapping("/api/v1/inventory/reserve/{reservationId}/confirm")
    ResponseEntity<Void> confirmInventoryReservation(@PathVariable("reservationId") String reservationId);

    /**
     * Get inventory details by product ID
     */
    @GetMapping("/api/v1/inventory/products/{productId}")
    ResponseEntity<InventoryDto> getInventoryByProductId(@PathVariable("productId") Long productId);

    /**
     * Get inventory details by product SKU
     */
    @GetMapping("/api/v1/inventory/products/by-sku/{sku}")
    ResponseEntity<InventoryDto> getInventoryByProductSku(@PathVariable("sku") String sku);

    /**
     * Get inventory details for multiple products
     */
    @PostMapping("/api/v1/inventory/products/batch")
    ResponseEntity<List<InventoryDto>> getBatchInventory(@RequestBody List<Long> productIds);

    /**
     * Update inventory after shipment
     */
    @PostMapping("/api/v1/inventory/shipment-update")
    ResponseEntity<Void> updateInventoryAfterShipment(@RequestBody ShipmentInventoryUpdate request);

    /**
     * Get low stock alerts
     */
    @GetMapping("/api/v1/inventory/alerts/low-stock")
    ResponseEntity<List<LowStockAlert>> getLowStockAlerts();

    /**
     * Check if products are in stock for shipping
     */
    @PostMapping("/api/v1/inventory/shipping/validate")
    ResponseEntity<ShippingInventoryValidation> validateInventoryForShipping(@RequestBody ShippingValidationRequest request);

    // DTOs for requests/responses
    record ShipmentInventoryUpdate(
        Long orderId,
        String reservationId,
        List<ShipmentItem> items,
        String action // SHIP, CANCEL, RETURN
    ) {
        record ShipmentItem(
            Long productId,
            String productSku,
            Integer quantity,
            Long warehouseId
        ) {}
    }

    record LowStockAlert(
        Long productId,
        String productSku,
        String productName,
        Long warehouseId,
        String warehouseLocation,
        Integer currentStock,
        Integer reorderLevel,
        String alertLevel // LOW, CRITICAL, OUT_OF_STOCK
    ) {}

    record ShippingValidationRequest(
        Long orderId,
        List<ValidationItem> items
    ) {
        record ValidationItem(
            Long productId,
            String productSku,
            Integer quantity,
            Long preferredWarehouseId
        ) {}
    }

    record ShippingInventoryValidation(
        Boolean isValid,
        List<ValidationResult> validationResults,
        List<String> recommendations
    ) {
        record ValidationResult(
            Long productId,
            String productSku,
            Boolean isAvailable,
            Integer availableQuantity,
            Integer requestedQuantity,
            Long suggestedWarehouseId,
            String reason
        ) {}
    }
} 
