package com.winnguyen1905.shipping.persistance.entity;

import java.math.BigDecimal;
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
@Table(name = "shipping_rate_calculations")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EShippingRateCalculation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calculation_id")
    private Long calculationId;
    
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "from_zip", nullable = false, length = 20)
    private String fromZip;
    
    @Column(name = "to_zip", nullable = false, length = 20)
    private String toZip;
    
    @Column(name = "total_weight", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalWeight;
    
    @Column(name = "total_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalValue;
    
    @Column(name = "package_count")
    @Builder.Default
    private Integer packageCount = ShippingConstants.DEFAULT_PACKAGE_COUNT;
    
    @Column(name = "requested_service_type", length = 50)
    private String requestedServiceType;
    
    @Column(name = "calculated_rates", nullable = false, columnDefinition = "JSON")
    private String calculatedRates;
    
    @Column(name = "selected_method_id")
    private Integer selectedMethodId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
} 