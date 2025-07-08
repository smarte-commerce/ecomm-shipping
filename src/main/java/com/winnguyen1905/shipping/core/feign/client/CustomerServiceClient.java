package com.winnguyen1905.shipping.core.feign.client;

import com.winnguyen1905.shipping.core.feign.dto.CustomerDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
    name = "customer-service",
    url = "${microservices.customer-service.url:http://localhost:8083}",
    fallback = CustomerServiceClientFallback.class
)
@CircuitBreaker(name = "customer-service")
@Retry(name = "customer-service")
public interface CustomerServiceClient {

    /**
     * Get customer details by customer ID
     */
    @GetMapping("/api/v1/customers/{customerId}")
    ResponseEntity<CustomerDto> getCustomerById(@PathVariable("customerId") Long customerId);

    /**
     * Get customer details by username
     */
    @GetMapping("/api/v1/customers/by-username/{username}")
    ResponseEntity<CustomerDto> getCustomerByUsername(@PathVariable("username") String username);

    /**
     * Get customer details by email
     */
    @GetMapping("/api/v1/customers/by-email/{email}")
    ResponseEntity<CustomerDto> getCustomerByEmail(@PathVariable("email") String email);

    /**
     * Get customer shipping preferences
     */
    @GetMapping("/api/v1/customers/{customerId}/shipping-preferences")
    ResponseEntity<CustomerDto.ShippingPreferences> getCustomerShippingPreferences(@PathVariable("customerId") Long customerId);

    /**
     * Get customer notification preferences
     */
    @GetMapping("/api/v1/customers/{customerId}/notification-preferences")
    ResponseEntity<CustomerDto.NotificationPreferences> getCustomerNotificationPreferences(@PathVariable("customerId") Long customerId);

    /**
     * Get customer addresses
     */
    @GetMapping("/api/v1/customers/{customerId}/addresses")
    ResponseEntity<List<CustomerDto.AddressDto>> getCustomerAddresses(@PathVariable("customerId") Long customerId);

    /**
     * Get customer default shipping address
     */
    @GetMapping("/api/v1/customers/{customerId}/addresses/default-shipping")
    ResponseEntity<CustomerDto.AddressDto> getCustomerDefaultShippingAddress(@PathVariable("customerId") Long customerId);

    /**
     * Get customer default billing address
     */
    @GetMapping("/api/v1/customers/{customerId}/addresses/default-billing")
    ResponseEntity<CustomerDto.AddressDto> getCustomerDefaultBillingAddress(@PathVariable("customerId") Long customerId);

    /**
     * Validate customer for shipping
     */
    @GetMapping("/api/v1/customers/{customerId}/validate-shipping")
    ResponseEntity<CustomerValidationResult> validateCustomerForShipping(@PathVariable("customerId") Long customerId);

    /**
     * Get customer contact information
     */
    @GetMapping("/api/v1/customers/{customerId}/contact")
    ResponseEntity<CustomerContactInfo> getCustomerContactInfo(@PathVariable("customerId") Long customerId);

    /**
     * Update customer shipping preferences
     */
    @PutMapping("/api/v1/customers/{customerId}/shipping-preferences")
    ResponseEntity<CustomerDto.ShippingPreferences> updateShippingPreferences(
        @PathVariable("customerId") Long customerId,
        @RequestBody CustomerDto.ShippingPreferences preferences
    );

    // DTOs for responses
    record CustomerValidationResult(
        Long customerId,
        Boolean isValid,
        Boolean isActive,
        Boolean isVerified,
        Boolean hasValidAddress,
        List<String> validationErrors
    ) {}

    record CustomerContactInfo(
        Long customerId,
        String email,
        String phoneNumber,
        String preferredContactMethod,
        String timeZone,
        String preferredLanguage
    ) {}
} 
