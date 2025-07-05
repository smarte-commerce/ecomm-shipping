package com.winnguyen1905.shipping.persistance.repository;

import org.springframework.stereotype.Repository;

/**
 * Main shipping repository interface that extends ShipmentRepository
 * This provides the primary interface for shipping operations
 */
@Repository
public interface ShippingRepository extends ShipmentRepository {
    // This interface inherits all methods from ShipmentRepository
    // Additional custom methods can be added here if needed
}