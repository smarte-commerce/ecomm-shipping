package com.winnguyen1905.shipping.persistance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.winnguyen1905.shipping.persistance.entity.EShippingZone;

@Repository
public interface ShippingZoneRepository extends JpaRepository<EShippingZone, Integer> {
  Optional<EShippingZone> findByZoneCode(String zoneCode);

  List<EShippingZone> findByIsActive(Boolean isActive);

  List<EShippingZone> findByIsActiveOrderByZoneName(Boolean isActive);

  @Query("SELECT z FROM EShippingZone z WHERE z.isActive = true")
  List<EShippingZone> findAllActiveZones();

  @Query("SELECT z FROM EShippingZone z WHERE z.zoneName LIKE %:name%")
  List<EShippingZone> findByZoneNameContaining(@Param("name") String name);

  @Query("SELECT z FROM EShippingZone z WHERE z.isActive = true AND z.countries LIKE %:country%")
  List<EShippingZone> findByCountrySupported(@Param("country") String country);

  @Query("SELECT z FROM EShippingZone z WHERE z.isActive = true AND (z.zipCodes LIKE %:zip% OR z.zipCodes IS NULL)")
  List<EShippingZone> findByZipCodeSupported(@Param("zip") String zipCode);

  boolean existsByZoneCode(String zoneCode);

  boolean existsByZoneName(String zoneName);
}
