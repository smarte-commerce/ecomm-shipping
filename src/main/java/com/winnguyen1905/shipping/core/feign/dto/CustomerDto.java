package com.winnguyen1905.shipping.core.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

  private Long customerId;

  private String username;

  private String email;

  private String firstName;

  private String lastName;

  private String phoneNumber;

  private String dateOfBirth;

  private String customerType; // INDIVIDUAL, BUSINESS

  private String companyName;

  private String taxNumber;

  private String preferredLanguage;

  private String timeZone;

  private NotificationPreferences notificationPreferences;

  private ShippingPreferences shippingPreferences;

  private List<AddressDto> addresses;

  private Boolean isActive;

  private Boolean isVerified;

  private Instant createdAt;

  private Instant updatedAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NotificationPreferences {

    private Boolean emailNotifications;

    private Boolean smsNotifications;

    private Boolean pushNotifications;

    private Boolean shippingUpdates;

    private Boolean deliveryConfirmations;

    private Boolean marketingEmails;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ShippingPreferences {

    private String preferredCarrier;

    private String preferredServiceType;

    private Boolean signatureRequired;

    private Boolean weekendDelivery;

    private Boolean leaveAtDoor;

    private String deliveryInstructions;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AddressDto {

    private Long addressId;

    private String addressType; // HOME, WORK, BILLING, SHIPPING

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private Boolean isDefault;

    private Boolean isVerified;
  }
}
