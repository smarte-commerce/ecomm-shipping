package com.winnguyen1905.shipping.persistance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shipping_quotes")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EShippingQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_id")
    private Long quoteId;

    @Column(name = "external_quote_id", length = 100)
    private String externalQuoteId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "vendor_id", length = 50)
    private String vendorId;

    @Column(name = "vendor_name", length = 255)
    private String vendorName;

    @Column(name = "request_type", nullable = false, length = 20)
    private String requestType; // SINGLE_VENDOR, MULTI_VENDOR

    @Column(name = "origin_country", nullable = false, length = 3)
    private String originCountry;

    @Column(name = "destination_country", nullable = false, length = 3)
    private String destinationCountry;

    @Column(name = "total_weight", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalWeight;

    @Column(name = "total_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "package_count", nullable = false)
    @Builder.Default
    private Integer packageCount = 1;

    @Column(name = "is_domestic", nullable = false)
    @Builder.Default
    private Boolean isDomestic = false;

    @Column(name = "requires_customs", nullable = false)
    @Builder.Default
    private Boolean requiresCustoms = false;

    @Column(name = "request_data", columnDefinition = "JSON")
    private String requestData; // Serialized request for audit

    @Column(name = "response_data", columnDefinition = "JSON")
    private String responseData; // Serialized response for audit

    @Column(name = "best_option_provider", length = 50)
    private String bestOptionProvider;

    @Column(name = "best_option_service", length = 100)
    private String bestOptionService;

    @Column(name = "best_option_cost", precision = 10, scale = 2)
    private BigDecimal bestOptionCost;

    @Column(name = "best_option_currency", length = 3)
    private String bestOptionCurrency;

    @Column(name = "best_option_delivery_days")
    private Integer bestOptionDeliveryDays;

    @Column(name = "cheapest_option_provider", length = 50)
    private String cheapestOptionProvider;

    @Column(name = "cheapest_option_cost", precision = 10, scale = 2)
    private BigDecimal cheapestOptionCost;

    @Column(name = "fastest_option_provider", length = 50)
    private String fastestOptionProvider;

    @Column(name = "fastest_option_delivery_days")
    private Integer fastestOptionDeliveryDays;

    @Column(name = "total_options_count", nullable = false)
    @Builder.Default
    private Integer totalOptionsCount = 0;

    @Column(name = "providers_queried", columnDefinition = "JSON")
    private String providersQueried; // List of providers that were queried

    @Column(name = "providers_responded", columnDefinition = "JSON")
    private String providersResponded; // List of providers that responded successfully

    @Column(name = "provider_errors", columnDefinition = "JSON")
    private String providerErrors; // List of provider errors

    @Column(name = "calculation_method", length = 50)
    private String calculationMethod; // EXTERNAL_API, INTERNAL_CALCULATION, HYBRID

    @Column(name = "cache_hit", nullable = false)
    @Builder.Default
    private Boolean cacheHit = false;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "selected_option_id", length = 100)
    private String selectedOptionId; // Set when user selects a shipping option

    @Column(name = "selected_provider", length = 50)
    private String selectedProvider;

    @Column(name = "selected_service", length = 100)
    private String selectedService;

    @Column(name = "selected_cost", precision = 10, scale = 2)
    private BigDecimal selectedCost;

    @Column(name = "selection_timestamp")
    private Instant selectionTimestamp;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false; // Whether this quote was used for an actual shipment

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_shipment_id")
    private EShipment relatedShipment; // Link to shipment if quote was used

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isSelected() {
        return selectedOptionId != null && selectionTimestamp != null;
    }

    public String getRouteDescription() {
        return originCountry + " -> " + destinationCountry + (isDomestic ? " (Domestic)" : " (International)");
    }
} 
