package com.winnguyen1905.shipping.core.feign.client;

import com.winnguyen1905.shipping.core.feign.dto.InventoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class InventoryServiceClientFallback implements InventoryServiceClient {

    @Override
    public ResponseEntity<InventoryDto.AvailabilityResponse> checkAvailability(InventoryDto.AvailabilityRequest request) {
        log.warn("Inventory service is unavailable. Using fallback for checkAvailability");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<InventoryDto.ReservationResponse> reserveInventory(InventoryDto.ReservationRequest request) {
        log.warn("Inventory service is unavailable. Using fallback for reserveInventory");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<Void> releaseInventoryReservation(String reservationId) {
        log.warn("Inventory service is unavailable. Using fallback for releaseInventoryReservation: {}", reservationId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<Void> confirmInventoryReservation(String reservationId) {
        log.warn("Inventory service is unavailable. Using fallback for confirmInventoryReservation: {}", reservationId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<InventoryDto> getInventoryByProductId(Long productId) {
        log.warn("Inventory service is unavailable. Using fallback for getInventoryByProductId: {}", productId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<InventoryDto> getInventoryByProductSku(String sku) {
        log.warn("Inventory service is unavailable. Using fallback for getInventoryByProductSku: {}", sku);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<List<InventoryDto>> getBatchInventory(List<Long> productIds) {
        log.warn("Inventory service is unavailable. Using fallback for getBatchInventory: {}", productIds);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<Void> updateInventoryAfterShipment(ShipmentInventoryUpdate request) {
        log.warn("Inventory service is unavailable. Using fallback for updateInventoryAfterShipment");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<List<LowStockAlert>> getLowStockAlerts() {
        log.warn("Inventory service is unavailable. Using fallback for getLowStockAlerts");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<ShippingInventoryValidation> validateInventoryForShipping(ShippingValidationRequest request) {
        log.warn("Inventory service is unavailable. Using fallback for validateInventoryForShipping");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
} 
