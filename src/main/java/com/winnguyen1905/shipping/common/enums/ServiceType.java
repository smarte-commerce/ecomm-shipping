package com.winnguyen1905.shipping.common.enums;

/**
 * Enumeration representing different types of shipping services
 */
public enum ServiceType {
    STANDARD("Standard shipping"),
    EXPRESS("Express shipping (2-3 days)"),
    OVERNIGHT("Next-day delivery"),
    SAME_DAY("Same-day delivery"),
    ECONOMY("Economy shipping");
    
    private final String description;
    
    ServiceType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 