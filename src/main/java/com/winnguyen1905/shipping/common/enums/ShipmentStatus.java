package com.winnguyen1905.shipping.common.enums;

/**
 * Enumeration representing different states of a shipment
 */
public enum ShipmentStatus {
    PENDING("Pending - Initial state"),
    LABEL_CREATED("Label Created - Shipping label generated"),
    PICKED_UP("Picked Up - Package picked up by carrier"),
    IN_TRANSIT("In Transit - Package in transit"),
    OUT_FOR_DELIVERY("Out for Delivery - Package out for delivery"),
    DELIVERED("Delivered - Package delivered"),
    FAILED("Failed - Delivery failed"),
    CANCELLED("Cancelled - Shipment cancelled"),
    RETURNED("Returned - Package returned to sender");
    
    private final String description;
    
    ShipmentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if the status indicates the shipment is in a final state
     */
    public boolean isFinalStatus() {
        return this == DELIVERED || this == CANCELLED || this == RETURNED;
    }
    
    /**
     * Check if the status indicates the shipment is active/in progress
     */
    public boolean isActiveStatus() {
        return this == PICKED_UP || this == IN_TRANSIT || this == OUT_FOR_DELIVERY;
    }
} 