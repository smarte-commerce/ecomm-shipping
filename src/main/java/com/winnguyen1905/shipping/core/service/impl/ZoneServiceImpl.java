package com.winnguyen1905.shipping.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winnguyen1905.shipping.core.model.request.CreateZoneRequest;
import com.winnguyen1905.shipping.core.model.response.ZoneResponse;
import com.winnguyen1905.shipping.core.service.ZoneService;
import com.winnguyen1905.shipping.exception.BadRequestException;
import com.winnguyen1905.shipping.exception.BusinessLogicException;
import com.winnguyen1905.shipping.exception.ResourceNotFoundException;
import com.winnguyen1905.shipping.persistance.entity.EShippingZone;
import com.winnguyen1905.shipping.persistance.repository.ShippingZoneRepository;
import com.winnguyen1905.shipping.persistance.repository.ShippingMethodRepository;
import com.winnguyen1905.shipping.secure.TAccountRequest;
import com.winnguyen1905.shipping.util.ShippingValidationUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ZoneServiceImpl implements ZoneService {

    private final ShippingZoneRepository zoneRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ZoneResponse createZone(CreateZoneRequest request, TAccountRequest accountRequest) {
        log.info("Creating zone: {} for account: {}", request.getZoneName(), accountRequest.username());
        
        // Validate request
        validateCreateZoneRequest(request);
        
        // Check if zone code already exists
        if (zoneRepository.existsByZoneCode(request.getZoneCode())) {
            throw new BadRequestException("Zone code already exists: " + request.getZoneCode());
        }
        
        // Check if zone name already exists
        if (zoneRepository.existsByZoneName(request.getZoneName())) {
            throw new BadRequestException("Zone name already exists: " + request.getZoneName());
        }
        
        // Create zone entity
        EShippingZone zone = EShippingZone.builder()
                .zoneName(request.getZoneName())
                .zoneCode(request.getZoneCode().toUpperCase())
                .countries(convertListToJson(request.getCountries()))
                .statesProvinces(convertListToJson(request.getStatesProvinces()))
                .zipCodes(convertListToJson(request.getZipCodes()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        zone = zoneRepository.save(zone);
        
        log.info("Zone created successfully with ID: {}", zone.getZoneId());
        return mapToZoneResponse(zone);
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneResponse getZoneById(Integer id, TAccountRequest accountRequest) {
        log.info("Getting zone by ID: {} for account: {}", id, accountRequest.username());
        
        EShippingZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with ID: " + id));
        
        return mapToZoneResponse(zone);
    }

    @Override
    public ZoneResponse updateZone(Integer id, CreateZoneRequest request, TAccountRequest accountRequest) {
        log.info("Updating zone ID: {} for account: {}", id, accountRequest.username());
        
        // Validate request
        validateCreateZoneRequest(request);
        
        EShippingZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with ID: " + id));
        
        // Check if zone code already exists (excluding current zone)
        if (!zone.getZoneCode().equals(request.getZoneCode()) && 
            zoneRepository.existsByZoneCode(request.getZoneCode())) {
            throw new BadRequestException("Zone code already exists: " + request.getZoneCode());
        }
        
        // Check if zone name already exists (excluding current zone)
        if (!zone.getZoneName().equals(request.getZoneName()) && 
            zoneRepository.existsByZoneName(request.getZoneName())) {
            throw new BadRequestException("Zone name already exists: " + request.getZoneName());
        }
        
        // Update zone
        zone.setZoneName(request.getZoneName());
        zone.setZoneCode(request.getZoneCode().toUpperCase());
        zone.setCountries(convertListToJson(request.getCountries()));
        zone.setStatesProvinces(convertListToJson(request.getStatesProvinces()));
        zone.setZipCodes(convertListToJson(request.getZipCodes()));
        zone.setIsActive(request.getIsActive() != null ? request.getIsActive() : zone.getIsActive());
        
        zone = zoneRepository.save(zone);
        
        log.info("Zone updated successfully with ID: {}", zone.getZoneId());
        return mapToZoneResponse(zone);
    }

    @Override
    public void deleteZone(Integer id, TAccountRequest accountRequest) {
        log.info("Deleting zone ID: {} for account: {}", id, accountRequest.username());
        
        EShippingZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with ID: " + id));
        
        // Check if zone has active shipping methods
        long activeMethodsCount = shippingMethodRepository.countByZoneZoneIdAndIsActive(id, true);
        if (activeMethodsCount > 0) {
            throw new BusinessLogicException("Cannot delete zone with active shipping methods");
        }
        
        // Soft delete by deactivating
        zone.setIsActive(false);
        zoneRepository.save(zone);
        
        log.info("Zone deactivated successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ZoneResponse> getAllZones(Boolean isActive, String name, String country, 
                                        Pageable pageable, TAccountRequest accountRequest) {
        log.info("Getting all zones with filters - isActive: {}, name: {}, country: {} for account: {}", 
                isActive, name, country, accountRequest.username());
        
        // Build query based on filters
        List<EShippingZone> zones;
        
        if (isActive != null) {
            zones = zoneRepository.findByIsActiveOrderByZoneName(isActive);
        } else {
            zones = zoneRepository.findAll();
        }
        
        // Apply additional filters
        if (name != null && !name.trim().isEmpty()) {
            zones = zones.stream()
                    .filter(z -> z.getZoneName().toLowerCase().contains(name.toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (country != null && !country.trim().isEmpty()) {
            zones = zones.stream()
                    .filter(z -> {
                        List<String> countries = convertJsonToList(z.getCountries());
                        return countries.stream()
                                .anyMatch(c -> c.toLowerCase().contains(country.toLowerCase()));
                    })
                    .collect(Collectors.toList());
        }
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), zones.size());
        
        List<ZoneResponse> zoneResponses = zones.subList(start, end).stream()
                .map(this::mapToZoneResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(zoneResponses, pageable, zones.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZoneResponse> getActiveZones(TAccountRequest accountRequest) {
        log.info("Getting active zones for account: {}", accountRequest.username());
        
        List<EShippingZone> zones = zoneRepository.findAllActiveZones();
        
        return zones.stream()
                .map(this::mapToZoneResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZoneResponse> getZonesByCountry(String country, TAccountRequest accountRequest) {
        log.info("Getting zones for country: {} for account: {}", country, accountRequest.username());
        
        if (country == null || country.trim().isEmpty()) {
            throw new BadRequestException("Country cannot be null or empty");
        }
        
        List<EShippingZone> zones = zoneRepository.findByCountrySupported(country.toLowerCase());
        
        return zones.stream()
                .map(this::mapToZoneResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneResponse lookupZone(String country, String state, String postalCode, TAccountRequest accountRequest) {
        log.info("Looking up zone for country: {}, state: {}, postal: {} for account: {}", 
                country, state, postalCode, accountRequest.username());
        
        if (country == null || country.trim().isEmpty()) {
            throw new BadRequestException("Country cannot be null or empty");
        }
        
        List<EShippingZone> zones = zoneRepository.findAllActiveZones();
        
        // Find zones that match the criteria
        List<EShippingZone> matchingZones = zones.stream()
                .filter(zone -> isZoneMatchingAddress(zone, country, state, postalCode))
                .collect(Collectors.toList());
        
        if (matchingZones.isEmpty()) {
            throw new ResourceNotFoundException("No zone found for the given address");
        }
        
        // Return the most specific match (prioritize zones with specific postal codes)
        EShippingZone bestMatch = findBestMatchingZone(matchingZones, country, state, postalCode);
        
        log.info("Found zone: {} for address", bestMatch.getZoneName());
        return mapToZoneResponse(bestMatch);
    }

    private void validateCreateZoneRequest(CreateZoneRequest request) {
        ShippingValidationUtils.validateNotNull(request, "Zone request cannot be null");
        ShippingValidationUtils.validateNotBlank(request.getZoneName(), "Zone name cannot be blank");
        ShippingValidationUtils.validateNotBlank(request.getZoneCode(), "Zone code cannot be blank");
        ShippingValidationUtils.validateNotNull(request.getCountries(), "Countries cannot be null");
        
        if (request.getCountries().isEmpty()) {
            throw new BadRequestException("At least one country must be specified");
        }
        
        // Validate that all countries are valid (simplified validation)
        for (String country : request.getCountries()) {
            if (country == null || country.trim().isEmpty()) {
                throw new BadRequestException("Country cannot be null or empty");
            }
        }
    }

    private boolean isZoneMatchingAddress(EShippingZone zone, String country, String state, String postalCode) {
        // Check if country matches
        List<String> zoneCountries = convertJsonToList(zone.getCountries());
        boolean countryMatch = zoneCountries.stream()
                .anyMatch(c -> c.equalsIgnoreCase(country));
        
        if (!countryMatch) {
            return false;
        }
        
        // Check if state matches (if specified)
        if (state != null && !state.trim().isEmpty()) {
            List<String> zoneStates = convertJsonToList(zone.getStatesProvinces());
            if (!zoneStates.isEmpty()) {
                boolean stateMatch = zoneStates.stream()
                        .anyMatch(s -> s.equalsIgnoreCase(state));
                if (!stateMatch) {
                    return false;
                }
            }
        }
        
        // Check if postal code matches (if specified)
        if (postalCode != null && !postalCode.trim().isEmpty()) {
            List<String> zoneZipCodes = convertJsonToList(zone.getZipCodes());
            if (!zoneZipCodes.isEmpty()) {
                boolean zipMatch = zoneZipCodes.stream()
                        .anyMatch(zip -> isPostalCodeMatching(zip, postalCode));
                if (!zipMatch) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private boolean isPostalCodeMatching(String zoneZip, String postalCode) {
        // Simple postal code matching logic
        // This could be enhanced with more sophisticated pattern matching
        
        // Exact match
        if (zoneZip.equalsIgnoreCase(postalCode)) {
            return true;
        }
        
        // Range matching (e.g., "10000-19999")
        if (zoneZip.contains("-")) {
            String[] parts = zoneZip.split("-");
            if (parts.length == 2) {
                try {
                    int zipStart = Integer.parseInt(parts[0]);
                    int zipEnd = Integer.parseInt(parts[1]);
                    int postal = Integer.parseInt(postalCode);
                    return postal >= zipStart && postal <= zipEnd;
                } catch (NumberFormatException e) {
                    // If parsing fails, fall back to string comparison
                    return postalCode.startsWith(parts[0]);
                }
            }
        }
        
        // Prefix matching (e.g., "100*" matches "10001", "10002", etc.)
        if (zoneZip.endsWith("*")) {
            String prefix = zoneZip.substring(0, zoneZip.length() - 1);
            return postalCode.startsWith(prefix);
        }
        
        return false;
    }

    private EShippingZone findBestMatchingZone(List<EShippingZone> zones, String country, String state, String postalCode) {
        // Scoring system to find the best match
        // Higher score = better match
        
        EShippingZone bestMatch = zones.get(0);
        int bestScore = 0;
        
        for (EShippingZone zone : zones) {
            int score = 0;
            
            // Base score for country match
            score += 10;
            
            // Bonus for state match
            if (state != null && !state.trim().isEmpty()) {
                List<String> zoneStates = convertJsonToList(zone.getStatesProvinces());
                if (zoneStates.stream().anyMatch(s -> s.equalsIgnoreCase(state))) {
                    score += 20;
                }
            }
            
            // Bonus for postal code match
            if (postalCode != null && !postalCode.trim().isEmpty()) {
                List<String> zoneZipCodes = convertJsonToList(zone.getZipCodes());
                if (zoneZipCodes.stream().anyMatch(zip -> isPostalCodeMatching(zip, postalCode))) {
                    score += 30;
                }
            }
            
            // Bonus for more specific zones (fewer countries)
            List<String> zoneCountries = convertJsonToList(zone.getCountries());
            if (zoneCountries.size() == 1) {
                score += 5;
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestMatch = zone;
            }
        }
        
        return bestMatch;
    }

    private String convertListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert list to JSON", e);
            return "[]";
        }
    }

    private List<String> convertJsonToList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to list: {}", json, e);
            return new ArrayList<>();
        }
    }

    private ZoneResponse mapToZoneResponse(EShippingZone zone) {
        // Get shipping methods count for the zone
        int methodsCount = shippingMethodRepository.findByZoneZoneIdAndIsActive(zone.getZoneId(), true).size();
        
        return ZoneResponse.builder()
                .zoneId(zone.getZoneId())
                .zoneName(zone.getZoneName())
                .zoneCode(zone.getZoneCode())
                .countries(convertJsonToList(zone.getCountries()))
                .statesProvinces(convertJsonToList(zone.getStatesProvinces()))
                .zipCodes(convertJsonToList(zone.getZipCodes()))
                .isActive(zone.getIsActive())
                .createdAt(zone.getCreatedAt())
                .updatedAt(zone.getUpdatedAt())
                .shippingMethodsCount(methodsCount)
                .build();
    }
} 
