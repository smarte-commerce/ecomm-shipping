package com.winnguyen1905.shipping.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.winnguyen1905.shipping.core.model.request.CreateShippingMethodRequest;
import com.winnguyen1905.shipping.core.model.response.ShippingMethodResponse;
import com.winnguyen1905.shipping.common.enums.ServiceType;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingMethodService {

    /**
     * Creates a new shipping method
     * @param request The shipping method creation request
     * @param accountRequest The account request for authorization
     * @return The created shipping method response
     */
    ShippingMethodResponse createShippingMethod(CreateShippingMethodRequest request, TAccountRequest accountRequest);

    /**
     * Retrieves a shipping method by its ID
     * @param id The shipping method ID
     * @param accountRequest The account request for authorization
     * @return The shipping method response
     */
    ShippingMethodResponse getShippingMethodById(Integer id, TAccountRequest accountRequest);

    /**
     * Updates an existing shipping method
     * @param id The shipping method ID
     * @param request The shipping method update request
     * @param accountRequest The account request for authorization
     * @return The updated shipping method response
     */
    ShippingMethodResponse updateShippingMethod(Integer id, CreateShippingMethodRequest request, TAccountRequest accountRequest);

    /**
     * Deletes a shipping method
     * @param id The shipping method ID
     * @param accountRequest The account request for authorization
     */
    void deleteShippingMethod(Integer id, TAccountRequest accountRequest);

    /**
     * Retrieves all shipping methods with optional filtering
     * @param carrierId Filter by carrier ID
     * @param zoneId Filter by zone ID
     * @param serviceType Filter by service type
     * @param isActive Filter by active status
     * @param pageable Pagination parameters
     * @param accountRequest The account request for authorization
     * @return Paginated list of shipping method responses
     */
    Page<ShippingMethodResponse> getAllShippingMethods(Integer carrierId, Integer zoneId, ServiceType serviceType, 
                                                      Boolean isActive, Pageable pageable, TAccountRequest accountRequest);

    /**
     * Retrieves all shipping methods for a specific carrier
     * @param carrierId The carrier ID
     * @param accountRequest The account request for authorization
     * @return List of shipping method responses
     */
    List<ShippingMethodResponse> getShippingMethodsByCarrier(Integer carrierId, TAccountRequest accountRequest);

    /**
     * Retrieves all shipping methods for a specific zone
     * @param zoneId The zone ID
     * @param accountRequest The account request for authorization
     * @return List of shipping method responses
     */
    List<ShippingMethodResponse> getShippingMethodsByZone(Integer zoneId, TAccountRequest accountRequest);

    /**
     * Retrieves available shipping methods for given criteria
     * @param carrierId The carrier ID (optional)
     * @param zoneId The zone ID (optional)
     * @param weight The shipment weight
     * @param orderValue The order value
     * @param accountRequest The account request for authorization
     * @return List of available shipping method responses
     */
    List<ShippingMethodResponse.ShippingMethodAvailability> getAvailableShippingMethods(
        Integer carrierId, Integer zoneId, BigDecimal weight, BigDecimal orderValue, TAccountRequest accountRequest);

    /**
     * Activates a shipping method
     * @param id The shipping method ID
     * @param accountRequest The account request for authorization
     * @return The updated shipping method response
     */
    ShippingMethodResponse activateShippingMethod(Integer id, TAccountRequest accountRequest);

    /**
     * Deactivates a shipping method
     * @param id The shipping method ID
     * @param accountRequest The account request for authorization
     * @return The updated shipping method response
     */
    ShippingMethodResponse deactivateShippingMethod(Integer id, TAccountRequest accountRequest);

    /**
     * Retrieves shipping method statistics
     * @param id The shipping method ID
     * @param accountRequest The account request for authorization
     * @return Shipping method statistics
     */
    ShippingMethodResponse.ShippingMethodStatistics getShippingMethodStatistics(Integer id, TAccountRequest accountRequest);

    /**
     * Bulk updates shipping method rates
     * @param methodIds List of shipping method IDs
     * @param rateMultiplier Multiplier to apply to rates
     * @param accountRequest The account request for authorization
     * @return Bulk update result
     */
    ShippingMethodResponse.BulkUpdateResult bulkUpdateRates(List<Integer> methodIds, BigDecimal rateMultiplier, 
                                                           TAccountRequest accountRequest);

    /**
     * Clones a shipping method to a different zone
     * @param methodId The source shipping method ID
     * @param targetZoneId The target zone ID
     * @param accountRequest The account request for authorization
     * @return The cloned shipping method response
     */
    ShippingMethodResponse cloneShippingMethod(Integer methodId, Integer targetZoneId, TAccountRequest accountRequest);
} 
