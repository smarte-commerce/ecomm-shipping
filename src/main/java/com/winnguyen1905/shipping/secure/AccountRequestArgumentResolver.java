package com.winnguyen1905.shipping.secure;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AccountRequestArgumentResolver implements HandlerMethodArgumentResolver {

  public static enum AccountRequestArgument {
    ID("sub"), USERNAME("username"), ROLE("role"), REGION("region");

    String value;

    AccountRequestArgument(String value) {
      this.value = value;
    }
  };

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(AccountRequest.class) &&
        parameter.getParameterType().equals(TAccountRequest.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
    
    log.debug("Resolving account request argument from headers and authentication context");
    
    // Extract region using the enhanced multi-factor detection
    RegionPartition region = extractRegionWithMultiFactorDetection(webRequest);
    
    // Extract other account information
    String username = null;
    UUID id = null;
    AccountType accountType = AccountType.CUSTOMER; // Default
    UUID socketClientId = null;
    
    try {
      // Try to extract from JWT token if available
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        
        username = jwt.getClaimAsString(AccountRequestArgument.USERNAME.value);
        id = UUID.fromString(jwt.getClaimAsString(AccountRequestArgument.ID.value));
        accountType = AccountType.valueOf(jwt.getClaimAsString(AccountRequestArgument.ROLE.value));
      }
    } catch (Exception e) {
      log.debug("No valid JWT authentication found, using header-based approach: {}", e.getMessage());
    }
    
    // Fallback to headers if JWT is not available
    if (username == null) {
      username = webRequest.getHeader("X-User-Username");
    }
    if (id == null) {
      String userIdHeader = webRequest.getHeader("X-User-ID");
      if (userIdHeader != null) {
        try {
          id = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
          log.warn("Invalid user ID in header: {}", userIdHeader);
          id = UUID.randomUUID(); // Generate temporary ID
        }
      } else {
        id = UUID.randomUUID(); // Generate temporary ID for anonymous users
      }
    }
    if (accountType == AccountType.CUSTOMER) {
      String roleHeader = webRequest.getHeader("X-User-Role");
      if (roleHeader != null) {
        try {
          accountType = AccountType.valueOf(roleHeader.toUpperCase());
        } catch (IllegalArgumentException e) {
          log.warn("Invalid account type in header: {}", roleHeader);
        }
      }
    }

    // Extract socket client ID if available
    String socketHeader = webRequest.getHeader("X-Socket-Client-ID");
    if (socketHeader != null) {
      try {
        socketClientId = UUID.fromString(socketHeader);
      } catch (IllegalArgumentException e) {
        log.warn("Invalid socket client ID in header: {}", socketHeader);
      }
    }

    log.info("Resolved account request - User: {}, Region: {}, Type: {}, Detection: {}", 
             username, region.getCode(), accountType, 
             webRequest.getHeader("X-Region-Detection-Method"));

    return TAccountRequest.builder()
        .id(id)
        .region(region)
        .username(username)
        .accountType(accountType)
        .socketClientId(socketClientId)
        .build();
  }

  /**
   * Enhanced region extraction using multiple detection factors with priority order
   */
  private RegionPartition extractRegionWithMultiFactorDetection(NativeWebRequest webRequest) {
    
    // Priority 1: Explicit region preference header (highest priority)
    String preferredRegion = webRequest.getHeader("X-Preferred-Region");
    if (isValidRegionCode(preferredRegion)) {
      log.debug("Using preferred region from header: {}", preferredRegion);
      return RegionPartition.fromCode(preferredRegion);
    }
    
    // Priority 2: Gateway-detected region header (most reliable)
    String gatewayRegion = webRequest.getHeader("X-Region-Code");
    if (isValidRegionCode(gatewayRegion)) {
      String detectionMethod = webRequest.getHeader("X-Region-Detection-Method");
      log.debug("Using gateway-detected region: {} via {}", gatewayRegion, detectionMethod);
      return RegionPartition.fromCode(gatewayRegion);
    }

    // Priority 3: User region from authentication context (JWT token)
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String jwtRegion = jwt.getClaimAsString(AccountRequestArgument.REGION.value);
        if (isValidRegionCode(jwtRegion)) {
          log.debug("Using region from JWT token: {}", jwtRegion);
          return RegionPartition.fromCode(jwtRegion);
        }
      }
    } catch (Exception e) {
      log.debug("No JWT token available for region extraction: {}", e.getMessage());
    }
    
    // Priority 4: Accept-Language header analysis
    String acceptLanguage = webRequest.getHeader("Accept-Language");
    if (acceptLanguage != null && !acceptLanguage.trim().isEmpty()) {
      RegionPartition languageRegion = extractRegionFromLanguage(acceptLanguage);
      if (languageRegion != null) {
        log.debug("Using region from Accept-Language header: {}", languageRegion);
        return languageRegion;
      }
    }
    
    // Priority 5: Client IP geolocation (if available from gateway)
    String clientIp = webRequest.getHeader("X-Client-IP");
    if (clientIp != null && !clientIp.trim().isEmpty() && !"unknown".equals(clientIp)) {
      RegionPartition ipRegion = extractRegionFromIp(clientIp);
      if (ipRegion != null) {
        log.debug("Using region from client IP {}: {}", clientIp, ipRegion);
        return ipRegion;
      }
    }
    
    // Priority 6: Session-based cached region
    String sessionRegion = webRequest.getHeader("X-Session-Region");
    if (isValidRegionCode(sessionRegion)) {
      log.debug("Using cached session region: {}", sessionRegion);
      return RegionPartition.fromCode(sessionRegion);
    }
    
    // Default fallback
    log.debug("No region detected, defaulting to US");
    return RegionPartition.US;
  }

  /**
   * Extract region from Accept-Language header
   */
  private RegionPartition extractRegionFromLanguage(String acceptLanguage) {
    try {
      // Simple language to region mapping
      if (acceptLanguage.contains("en-US") || acceptLanguage.contains("en-CA") || acceptLanguage.contains("es-MX")) {
        return RegionPartition.US;
      } else if (acceptLanguage.contains("en-GB") || acceptLanguage.contains("de") || 
                 acceptLanguage.contains("fr") || acceptLanguage.contains("it") || 
                 acceptLanguage.contains("es-ES")) {
        return RegionPartition.EU;
      } else if (acceptLanguage.contains("zh") || acceptLanguage.contains("ja") || 
                 acceptLanguage.contains("ko") || acceptLanguage.contains("en-AU") || 
                 acceptLanguage.contains("en-SG")) {
        return RegionPartition.ASIA;
      }
    } catch (Exception e) {
      log.debug("Error parsing Accept-Language header: {}", e.getMessage());
    }
    return null;
  }

  /**
   * Simple IP-based region detection
   */
  private RegionPartition extractRegionFromIp(String ip) {
    try {
      // This is a simplified version - in production you'd use a proper GeoIP service
      if (ip.startsWith("192.168") || ip.startsWith("10.") || ip.startsWith("172.")) {
        return RegionPartition.US; // Local IPs default to US
      }
      
      // Add more sophisticated IP ranges here
      // For now, just return null to proceed to next detection method
      return null;
    } catch (Exception e) {
      log.debug("Error detecting region from IP: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Validate if region code is valid
   */
  private boolean isValidRegionCode(String regionCode) {
    if (regionCode == null || regionCode.trim().isEmpty()) {
      return false;
    }
    try {
      RegionPartition.fromCode(regionCode);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
