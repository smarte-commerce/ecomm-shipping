package com.winnguyen1905.shipping.persistance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.winnguyen1905.shipping.persistance.entity.EShippingCarrier;

@Repository
public interface ShippingCarrierRepository extends JpaRepository<EShippingCarrier, Integer> {
    
    Optional<EShippingCarrier> findByCarrierCode(String carrierCode);
    
    List<EShippingCarrier> findByIsActive(Boolean isActive);
    
    List<EShippingCarrier> findByIsActiveOrderByCarrierName(Boolean isActive);
    
    @Query("SELECT c FROM EShippingCarrier c WHERE c.isActive = true")
    List<EShippingCarrier> findAllActiveCarriers();
    
    @Query("SELECT c FROM EShippingCarrier c WHERE c.carrierName LIKE %:name%")
    List<EShippingCarrier> findByCarrierNameContaining(@Param("name") String name);
    
    @Query("SELECT c FROM EShippingCarrier c WHERE c.isActive = true AND c.supportedCountries LIKE %:country%")
    List<EShippingCarrier> findByCountrySupported(@Param("country") String country);
    
    boolean existsByCarrierCode(String carrierCode);
    
    boolean existsByCarrierName(String carrierName);
} 