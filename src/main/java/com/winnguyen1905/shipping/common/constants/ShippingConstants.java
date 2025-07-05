package com.winnguyen1905.shipping.common.constants;

import java.math.BigDecimal;

/**
 * Constants used throughout the shipping service
 */
public final class ShippingConstants {
    
    private ShippingConstants() {
        // Utility class - prevent instantiation
    }
    
    // Default values for shipping methods
    public static final BigDecimal DEFAULT_BASE_RATE = BigDecimal.ZERO;
    public static final BigDecimal DEFAULT_PER_KG_RATE = BigDecimal.ZERO;
    public static final BigDecimal DEFAULT_PER_ITEM_RATE = BigDecimal.ZERO;
    public static final BigDecimal DEFAULT_MIN_WEIGHT = BigDecimal.ZERO;
    public static final BigDecimal DEFAULT_MAX_WEIGHT = new BigDecimal("999.99");
    public static final BigDecimal DEFAULT_MIN_ORDER_VALUE = BigDecimal.ZERO;
    public static final BigDecimal DEFAULT_MAX_ORDER_VALUE = new BigDecimal("999999.99");
    public static final Integer DEFAULT_MIN_DELIVERY_DAYS = 1;
    public static final Integer DEFAULT_MAX_DELIVERY_DAYS = 7;
    
    // Default values for shipments
    public static final Integer DEFAULT_PACKAGE_COUNT = 1;
    public static final BigDecimal DEFAULT_INSURANCE_COST = BigDecimal.ZERO;
    
    // Default values for packages
    public static final String DEFAULT_PACKAGE_TYPE = "box";
    public static final Boolean DEFAULT_IS_FRAGILE = false;
    public static final Boolean DEFAULT_IS_LIQUID = false;
    public static final Boolean DEFAULT_IS_HAZARDOUS = false;
    
    // Default values for entities
    public static final Boolean DEFAULT_IS_ACTIVE = true;
    public static final Boolean DEFAULT_IS_PROCESSED = false;
    
    // Package types
    public static final String PACKAGE_TYPE_BOX = "box";
    public static final String PACKAGE_TYPE_ENVELOPE = "envelope";
    public static final String PACKAGE_TYPE_TUBE = "tube";
    public static final String PACKAGE_TYPE_PAK = "pak";
    public static final String PACKAGE_TYPE_CUSTOM = "custom";
    
    // Common webhook types
    public static final String WEBHOOK_TYPE_TRACKING_UPDATE = "tracking_update";
    public static final String WEBHOOK_TYPE_DELIVERY_CONFIRMATION = "delivery_confirmation";
    public static final String WEBHOOK_TYPE_EXCEPTION = "exception";
    public static final String WEBHOOK_TYPE_PICKUP = "pickup";
    public static final String WEBHOOK_TYPE_IN_TRANSIT = "in_transit";
    
    // Weight limits (in kg)
    public static final BigDecimal MAX_STANDARD_WEIGHT = new BigDecimal("31.75"); // USPS limit
    public static final BigDecimal MAX_HEAVY_WEIGHT = new BigDecimal("70.00"); // UPS/FedEx limit
    
    // Value limits (in currency)
    public static final BigDecimal MAX_DECLARED_VALUE = new BigDecimal("50000.00");
    public static final BigDecimal MIN_INSURANCE_VALUE = new BigDecimal("100.00");
} 