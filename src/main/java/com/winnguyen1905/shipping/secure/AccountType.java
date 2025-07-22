package com.winnguyen1905.shipping.secure;

public enum AccountType {
  ADMIN("ADMIN"), CUSTOMER("CUSTOMER"), VENDOR("VENDOR");

  String role;

  AccountType(String role) {
    this.role = role; 
  }
  
  /**
   * Convert role string to AccountType with mapping support
   */
  public static AccountType fromRole(String role) {
    if (role == null || role.trim().isEmpty()) {
      return CUSTOMER; // Default
    }
    
    String normalizedRole = role.trim().toUpperCase();
    
    // Handle Keycloak role mappings
    return switch (normalizedRole) {
      case "ADMIN" -> ADMIN;
      case "MANAGER" -> ADMIN; // Map manager to admin for simplicity
      case "VENDOR" -> VENDOR;
      case "USER", "CUSTOMER" -> CUSTOMER; // Map user role to customer
      default -> {
        // Try standard valueOf as fallback
        try {
          yield valueOf(normalizedRole);
        } catch (IllegalArgumentException e) {
          yield CUSTOMER; // Default fallback
        }
      }
    };
  }
  
  public String getRole() {
    return role;
  }
}
