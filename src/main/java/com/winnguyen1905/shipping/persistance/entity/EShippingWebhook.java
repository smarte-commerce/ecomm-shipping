package com.winnguyen1905.shipping.persistance.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.winnguyen1905.shipping.common.constants.ShippingConstants;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "shipping_webhooks")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EShippingWebhook {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "webhook_id")
  private Long webhookId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shipment_id")
  private EShipment shipment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "carrier_id", nullable = false)
  private EShippingCarrier carrier;

  @Column(name = "webhook_type", nullable = false, length = 50)
  private String webhookType;

  @Column(name = "tracking_number", length = 100)
  private String trackingNumber;

  @Column(name = "webhook_data", nullable = false, columnDefinition = "JSON")
  private String webhookData;

  @Column(name = "processed")
  @Builder.Default
  private Boolean processed = ShippingConstants.DEFAULT_IS_PROCESSED;

  @CreationTimestamp
  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  @Column(name = "processed_at")
  private Instant processedAt;
}
