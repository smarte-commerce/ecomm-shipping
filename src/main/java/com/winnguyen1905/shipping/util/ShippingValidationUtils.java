package com.winnguyen1905.shipping.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import com.winnguyen1905.shipping.common.constants.ShippingConstants;
import com.winnguyen1905.shipping.common.enums.ShipmentStatus;
import com.winnguyen1905.shipping.exception.BadRequestException;

/**
 * Utility class for common shipping validation operations
 */
public final class ShippingValidationUtils {
    
    private ShippingValidationUtils() {
        // Utility class - prevent instantiation
    }
    
    // Regex patterns for validation
    private static final Pattern ZIP_CODE_PATTERN = Pattern.compile("^\\d{5}(-\\d{4})?$");
    private static final Pattern TRACKING_NUMBER_PATTERN = Pattern.compile("^[A-Za-z0-9]{6,30}$");
    private static final Pattern CARRIER_CODE_PATTERN = Pattern.compile("^[A-Z]{2,10}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    
    /**
     * Validates that an object is not null
     */
    public static void validateNotNull(Object value, String message) {
        if (value == null) {
            throw new BadRequestException(message);
        }
    }
    
    /**
     * Validates that a string is not null or blank
     */
    public static void validateNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(message);
        }
    }
    
    /**
     * Validates that a collection is not null or empty
     */
    public static void validateNotEmpty(java.util.Collection<?> value, String message) {
        if (value == null || value.isEmpty()) {
            throw new BadRequestException(message);
        }
    }
    
    /**
     * Validates if a weight is within acceptable limits
     */
    public static boolean isValidWeight(BigDecimal weight) {
        return weight != null && 
               weight.compareTo(BigDecimal.ZERO) > 0 && 
               weight.compareTo(ShippingConstants.MAX_HEAVY_WEIGHT) <= 0;
    }
    
    /**
     * Validates if a weight is within standard shipping limits
     */
    public static boolean isStandardWeight(BigDecimal weight) {
        return weight != null && 
               weight.compareTo(BigDecimal.ZERO) > 0 && 
               weight.compareTo(ShippingConstants.MAX_STANDARD_WEIGHT) <= 0;
    }
    
    /**
     * Validates if a declared value is within acceptable limits
     */
    public static boolean isValidDeclaredValue(BigDecimal value) {
        return value != null && 
               value.compareTo(BigDecimal.ZERO) >= 0 && 
               value.compareTo(ShippingConstants.MAX_DECLARED_VALUE) <= 0;
    }
    
    /**
     * Validates if a value qualifies for insurance
     */
    public static boolean requiresInsurance(BigDecimal value) {
        return value != null && 
               value.compareTo(ShippingConstants.MIN_INSURANCE_VALUE) >= 0;
    }
    
    /**
     * Validates ZIP code format (US)
     */
    public static boolean isValidZipCode(String zipCode) {
        return zipCode != null && ZIP_CODE_PATTERN.matcher(zipCode).matches();
    }
    
    /**
     * Validates tracking number format
     */
    public static boolean isValidTrackingNumber(String trackingNumber) {
        return trackingNumber != null && 
               !trackingNumber.trim().isEmpty() && 
               TRACKING_NUMBER_PATTERN.matcher(trackingNumber).matches();
    }
    
    /**
     * Validates carrier code format
     */
    public static boolean isValidCarrierCode(String carrierCode) {
        return carrierCode != null && 
               !carrierCode.trim().isEmpty() && 
               CARRIER_CODE_PATTERN.matcher(carrierCode).matches();
    }
    
    /**
     * Validates phone number format
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && 
               !phoneNumber.trim().isEmpty() && 
               PHONE_PATTERN.matcher(phoneNumber.replaceAll("\\s+", "")).matches();
    }
    
    /**
     * Checks if a shipment status transition is valid
     */
    public static boolean isValidStatusTransition(ShipmentStatus currentStatus, ShipmentStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        
        // Can't transition from final statuses
        if (currentStatus.isFinalStatus()) {
            return false;
        }
        
        // Can't go backwards in the normal flow
        return switch (currentStatus) {
            case PENDING -> newStatus == ShipmentStatus.LABEL_CREATED || 
                          newStatus == ShipmentStatus.CANCELLED;
            case LABEL_CREATED -> newStatus == ShipmentStatus.PICKED_UP || 
                                newStatus == ShipmentStatus.CANCELLED;
            case PICKED_UP -> newStatus == ShipmentStatus.IN_TRANSIT || 
                            newStatus == ShipmentStatus.FAILED;
            case IN_TRANSIT -> newStatus == ShipmentStatus.OUT_FOR_DELIVERY || 
                             newStatus == ShipmentStatus.FAILED;
            case OUT_FOR_DELIVERY -> newStatus == ShipmentStatus.DELIVERED || 
                                   newStatus == ShipmentStatus.FAILED ||
                                   newStatus == ShipmentStatus.RETURNED;
            case FAILED -> newStatus == ShipmentStatus.PICKED_UP || 
                         newStatus == ShipmentStatus.RETURNED ||
                         newStatus == ShipmentStatus.CANCELLED;
            default -> false;
        };
    }
    
    /**
     * Validates package type
     */
    public static boolean isValidPackageType(String packageType) {
        return packageType != null && 
               (packageType.equals(ShippingConstants.PACKAGE_TYPE_BOX) ||
                packageType.equals(ShippingConstants.PACKAGE_TYPE_ENVELOPE) ||
                packageType.equals(ShippingConstants.PACKAGE_TYPE_TUBE) ||
                packageType.equals(ShippingConstants.PACKAGE_TYPE_PAK) ||
                packageType.equals(ShippingConstants.PACKAGE_TYPE_CUSTOM));
    }
    
    /**
     * Calculates dimensional weight (length * width * height / 139 for inches)
     */
    public static BigDecimal calculateDimensionalWeight(BigDecimal length, BigDecimal width, BigDecimal height) {
        if (length == null || width == null || height == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal volume = length.multiply(width).multiply(height);
        return volume.divide(new BigDecimal("139"), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Validates email format
     */
    public static boolean isValidEmail(String email) {
        return email != null && 
               email.contains("@") && 
               email.contains(".") && 
               email.length() > 5;
    }
} 
