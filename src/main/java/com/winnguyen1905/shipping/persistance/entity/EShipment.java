package com.winnguyen1905.shipping.persistance.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winnguyen1905.shipping.common.enums.ShipmentStatus;
import com.winnguyen1905.shipping.common.constants.ShippingConstants;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EShipment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "shipment_id")
  private Long shipmentId;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Column(name = "shipment_number", nullable = false, unique = true, length = 50)
  private String shipmentNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "carrier_id", nullable = false)
  private EShippingCarrier carrier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "method_id", nullable = false)
  private EShippingMethod method;

  @Column(name = "tracking_number", length = 100)
  private String trackingNumber;

  @Column(name = "shipping_label_url", length = 500)
  private String shippingLabelUrl;

  @Column(name = "from_address", nullable = false, columnDefinition = "JSON")
  private String fromAddress;

  @Column(name = "to_address", nullable = false, columnDefinition = "JSON")
  private String toAddress;

  @Column(name = "package_count")
  @Builder.Default
  private Integer packageCount = ShippingConstants.DEFAULT_PACKAGE_COUNT;

  @Column(name = "total_weight", nullable = false, precision = 8, scale = 2)
  private BigDecimal totalWeight;

  @Column(name = "total_value", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalValue;

  @Column(name = "shipping_cost", nullable = false, precision = 10, scale = 2)
  private BigDecimal shippingCost;

  @Column(name = "insurance_cost", precision = 10, scale = 2)
  @Builder.Default
  private BigDecimal insuranceCost = ShippingConstants.DEFAULT_INSURANCE_COST;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  @Builder.Default
  private ShipmentStatus status = ShipmentStatus.PENDING;

  @Column(name = "shipped_at")
  private Instant shippedAt;

  @Column(name = "estimated_delivery_date")
  private LocalDate estimatedDeliveryDate;

  @Column(name = "actual_delivery_date")
  private LocalDate actualDeliveryDate;
  
  @Column(name = "pickup_date")
  private Instant pickupDate;
  
  @Column(name = "delivered_date")
  private Instant deliveredDate;

  @Column(name = "delivery_signature", length = 255)
  private String deliverySignature;

  @Column(name = "delivery_notes", columnDefinition = "TEXT")
  private String deliveryNotes;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;

  @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private Set<EShipmentItem> shipmentItems;

  @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private Set<EShipmentPackage> shipmentPackages;

  @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private Set<EShipmentTrackingEvent> trackingEvents;

  @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private Set<EShippingWebhook> webhooks;
}
