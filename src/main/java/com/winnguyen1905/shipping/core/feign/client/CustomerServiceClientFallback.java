package com.winnguyen1905.shipping.core.feign.client;

import com.winnguyen1905.shipping.core.feign.dto.CustomerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class CustomerServiceClientFallback implements CustomerServiceClient {

    @Override
    public ResponseEntity<CustomerDto> getCustomerById(Long customerId) {
        log.warn("Customer service is unavailable. Using fallback for getCustomerById: {}", customerId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<CustomerDto> getCustomerByUsername(String username) {
        log.warn("Customer service is unavailable. Using fallback for getCustomerByUsername: {}", username);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<CustomerDto> getCustomerByEmail(String email) {
        log.warn("Customer service is unavailable. Using fallback for getCustomerByEmail: {}", email);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<CustomerDto.ShippingPreferences> getCustomerShippingPreferences(Long customerId) {
        log.warn("Customer service is unavailable. Using fallback for getCustomerShippingPreferences: {}", customerId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<CustomerDto.NotificationPreferences> getCustomerNotificationPreferences(Long customerId) {
        log.warn("Customer service is unavailable. Using fallback for getCustomerNotificationPreferences: {}", customerId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<List<CustomerDto.AddressDto>> getCustomerAddresses(Long customerId) {
        log.warn("Customer service is unavailable. Using fallback for getCustomerAddresses: {}", customerId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<CustomerDto.AddressDto> getCustomerDefaultShippingAddress(Long customerId) {
        log.warn("Customer service is unavailable. Using fallback for getCustomerDefaultShippingAddress: {}", customerId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<CustomerDto.AddressDto> getCustomerDefaultBillingAddress(Long customerId) {
        log.warn("Customer service is unavailable. Using fallback for getCustomerDefaultBillingAddress: {}", customerId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<CustomerValidationResult> validateCustomerForShipping(Long customerId) {
        log.warn("Customer service is unavailable. Using fallback for validateCustomerForShipping: {}", customerId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<CustomerContactInfo> getCustomerContactInfo(Long customerId) {
        log.warn("Customer service is unavailable. Using fallback for getCustomerContactInfo: {}", customerId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<CustomerDto.ShippingPreferences> updateShippingPreferences(Long customerId, CustomerDto.ShippingPreferences preferences) {
        log.warn("Customer service is unavailable. Using fallback for updateShippingPreferences: {}", customerId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
} 
