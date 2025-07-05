package com.winnguyen1905.shipping.persistance.entity;

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
@Table(name = "shipping_zones")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EShippingZone {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Integer zoneId;
    
    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;
    
    @Column(name = "zone_code", nullable = false, unique = true, length = 20)
    private String zoneCode;
    
    @Column(name = "countries", nullable = false, columnDefinition = "JSON")
    private String countries;
    
    @Column(name = "states_provinces", columnDefinition = "JSON")
    private String statesProvinces;
    
    @Column(name = "zip_codes", columnDefinition = "JSON")
    private String zipCodes;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<EShippingMethod> shippingMethods;
} 