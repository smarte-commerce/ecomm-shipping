package com.winnguyen1905.shipping.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.winnguyen1905.shipping.core.model.request.CreateCarrierRequest;
import com.winnguyen1905.shipping.core.model.response.CarrierResponse;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import java.util.List;

public interface CarrierService {

    /**
     * Creates a new carrier
     * @param request The carrier creation request
     * @param accountRequest The account request for authorization
     * @return The created carrier response
     */
    CarrierResponse createCarrier(CreateCarrierRequest request, TAccountRequest accountRequest);

    /**
     * Retrieves a carrier by its ID
     * @param id The carrier ID
     * @param accountRequest The account request for authorization
     * @return The carrier response
     */
    CarrierResponse getCarrierById(Integer id, TAccountRequest accountRequest);

    /**
     * Updates an existing carrier
     * @param id The carrier ID
     * @param request The carrier update request
     * @param accountRequest The account request for authorization
     * @return The updated carrier response
     */
    CarrierResponse updateCarrier(Integer id, CreateCarrierRequest request, TAccountRequest accountRequest);

    /**
     * Deletes a carrier (soft delete)
     * @param id The carrier ID
     * @param accountRequest The account request for authorization
     */
    void deleteCarrier(Integer id, TAccountRequest accountRequest);

    /**
     * Retrieves all carriers with optional filtering
     * @param isActive Filter by active status
     * @param name Filter by carrier name
     * @param code Filter by carrier code
     * @param country Filter by supported country
     * @param pageable Pagination parameters
     * @param accountRequest The account request for authorization
     * @return Paginated list of carrier responses
     */
    Page<CarrierResponse> getAllCarriers(Boolean isActive, String name, String code, String country, 
                                        Pageable pageable, TAccountRequest accountRequest);

    /**
     * Retrieves all active carriers
     * @param accountRequest The account request for authorization
     * @return List of active carrier responses
     */
    List<CarrierResponse> getActiveCarriers(TAccountRequest accountRequest);

    /**
     * Activates a carrier
     * @param id The carrier ID
     * @param accountRequest The account request for authorization
     * @return The updated carrier response
     */
    CarrierResponse activateCarrier(Integer id, TAccountRequest accountRequest);

    /**
     * Deactivates a carrier
     * @param id The carrier ID
     * @param accountRequest The account request for authorization
     * @return The updated carrier response
     */
    CarrierResponse deactivateCarrier(Integer id, TAccountRequest accountRequest);

    /**
     * Retrieves all shipping methods for a specific carrier
     * @param id The carrier ID
     * @param accountRequest The account request for authorization
     * @return List of shipping method responses
     */
    List<CarrierResponse.ShippingMethodResponse> getCarrierShippingMethods(Integer id, TAccountRequest accountRequest);

    /**
     * Tests the connection to a carrier's API
     * @param id The carrier ID
     * @param accountRequest The account request for authorization
     * @return The connection test response
     */
    CarrierResponse.ConnectionTestResponse testCarrierConnection(Integer id, TAccountRequest accountRequest);
} 
