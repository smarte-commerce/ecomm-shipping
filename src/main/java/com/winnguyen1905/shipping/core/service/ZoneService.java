package com.winnguyen1905.shipping.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.winnguyen1905.shipping.core.model.request.CreateZoneRequest;
import com.winnguyen1905.shipping.core.model.response.ZoneResponse;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import java.util.List;

public interface ZoneService {

    /**
     * Creates a new shipping zone
     * @param request The zone creation request
     * @param accountRequest The account request for authorization
     * @return The created zone response
     */
    ZoneResponse createZone(CreateZoneRequest request, TAccountRequest accountRequest);

    /**
     * Retrieves a zone by its ID
     * @param id The zone ID
     * @param accountRequest The account request for authorization
     * @return The zone response
     */
    ZoneResponse getZoneById(Integer id, TAccountRequest accountRequest);

    /**
     * Updates an existing zone
     * @param id The zone ID
     * @param request The zone update request
     * @param accountRequest The account request for authorization
     * @return The updated zone response
     */
    ZoneResponse updateZone(Integer id, CreateZoneRequest request, TAccountRequest accountRequest);

    /**
     * Deletes a zone
     * @param id The zone ID
     * @param accountRequest The account request for authorization
     */
    void deleteZone(Integer id, TAccountRequest accountRequest);

    /**
     * Retrieves all zones with optional filtering
     * @param isActive Filter by active status
     * @param name Filter by zone name
     * @param country Filter by country
     * @param pageable Pagination parameters
     * @param accountRequest The account request for authorization
     * @return Paginated list of zone responses
     */
    Page<ZoneResponse> getAllZones(Boolean isActive, String name, String country, 
                                  Pageable pageable, TAccountRequest accountRequest);

    /**
     * Retrieves all active zones
     * @param accountRequest The account request for authorization
     * @return List of active zone responses
     */
    List<ZoneResponse> getActiveZones(TAccountRequest accountRequest);

    /**
     * Retrieves zones that serve a specific country
     * @param country The country code
     * @param accountRequest The account request for authorization
     * @return List of zone responses
     */
    List<ZoneResponse> getZonesByCountry(String country, TAccountRequest accountRequest);

    /**
     * Finds the appropriate zone for a given address
     * @param country The country code
     * @param state The state/province
     * @param postalCode The postal code
     * @param accountRequest The account request for authorization
     * @return The matching zone response
     */
    ZoneResponse lookupZone(String country, String state, String postalCode, TAccountRequest accountRequest);
} 
