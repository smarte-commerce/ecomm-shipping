package com.winnguyen1905.shipping.persistance.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "shipping_methods")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EShippingMethod {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "method_id")
    private Integer methodId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", nullable = false)
    private EShippingCarrier carrier;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private EShippingZone zone;
    
    @Column(name = "method_name", nullable = false, length = 100)
    private String methodName;
    
    @Column(name = "method_code", nullable = false, length = 50)
    private String methodCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;
    
    @Column(name = "base_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal baseRate = BigDecimal.ZERO;
    
    @Column(name = "per_kg_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal perKgRate = BigDecimal.ZERO;
    
    @Column(name = "per_item_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal perItemRate = BigDecimal.ZERO;
    
    @Column(name = "min_weight", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal minWeight = BigDecimal.ZERO;
    
    @Column(name = "max_weight", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal maxWeight = new BigDecimal("999.99");
    
    @Column(name = "min_order_value", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;
    
    @Column(name = "max_order_value", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal maxOrderValue = new BigDecimal("999999.99");
    
    @Column(name = "estimated_days_min")
    @Builder.Default
    private Integer estimatedDaysMin = 1;
    
    @Column(name = "estimated_days_max")
    @Builder.Default
    private Integer estimatedDaysMax = 7;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @OneToMany(mappedBy = "method", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<EShipment> shipments;
    
    public enum ServiceType {
        STANDARD, EXPRESS, OVERNIGHT, SAME_DAY, ECONOMY
    }
} 