package com.winnguyen1905.shipping.persistance.entity;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winnguyen1905.shipping.common.constants.ShippingConstants;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "shipping_carriers")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EShippingCarrier {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "carrier_id")
  private Integer carrierId;

  @Column(name = "carrier_name", nullable = false, unique = true, length = 100)
  private String carrierName;

  @Column(name = "carrier_code", nullable = false, unique = true, length = 20)
  private String carrierCode;

  @Column(name = "is_active")
  @Builder.Default
  private Boolean isActive = ShippingConstants.DEFAULT_IS_ACTIVE;

  @Column(name = "supported_countries", columnDefinition = "JSON")
  private String supportedCountries;

  @Column(name = "api_endpoint", length = 255)
  private String apiEndpoint;

  @Column(name = "configuration", columnDefinition = "JSON")
  private String configuration;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;

  @OneToMany(mappedBy = "carrier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private Set<EShippingMethod> shippingMethods;

  @OneToMany(mappedBy = "carrier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private Set<EShipment> shipments;

  @OneToMany(mappedBy = "carrier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private Set<EShippingWebhook> webhooks;
}
