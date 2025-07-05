package com.winnguyen1905.shipping.persistance.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "shipment_packages")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EShipmentPackage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "package_id")
    private Long packageId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private EShipment shipment;
    
    @Column(name = "package_number", nullable = false, length = 50)
    private String packageNumber;
    
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;
    
    @Column(name = "weight", nullable = false, precision = 8, scale = 2)
    private BigDecimal weight;
    
    @Column(name = "dimensions", nullable = false, columnDefinition = "JSON")
    private String dimensions;
    
    @Column(name = "package_type", length = 50)
    @Builder.Default
    private String packageType = "box";
    
    @Column(name = "is_fragile")
    @Builder.Default
    private Boolean isFragile = false;
    
    @Column(name = "is_liquid")
    @Builder.Default
    private Boolean isLiquid = false;
    
    @Column(name = "is_hazardous")
    @Builder.Default
    private Boolean isHazardous = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
} 