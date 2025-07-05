package com.winnguyen1905.shipping.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.winnguyen1905.shipping.core.model.request.CreateShipmentRequest;
import com.winnguyen1905.shipping.core.model.request.UpdateShipmentRequest;
import com.winnguyen1905.shipping.core.model.response.ShipmentResponse;
import com.winnguyen1905.shipping.common.enums.ShipmentStatus;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import java.util.List;

public interface ShipmentService {

    /**
     * Creates a new shipment
     * @param request The shipment creation request
     * @param accountRequest The account request for authorization
     * @return The created shipment response
     */
    ShipmentResponse createShipment(CreateShipmentRequest request, TAccountRequest accountRequest);

    /**
     * Retrieves a shipment by its ID
     * @param id The shipment ID
     * @param accountRequest The account request for authorization
     * @return The shipment response
     */
    ShipmentResponse getShipmentById(Long id, TAccountRequest accountRequest);

    /**
     * Updates an existing shipment
     * @param id The shipment ID
     * @param request The shipment update request
     * @param accountRequest The account request for authorization
     * @return The updated shipment response
     */
    ShipmentResponse updateShipment(Long id, UpdateShipmentRequest request, TAccountRequest accountRequest);

    /**
     * Cancels a shipment (soft delete)
     * @param id The shipment ID
     * @param accountRequest The account request for authorization
     */
    void cancelShipment(Long id, TAccountRequest accountRequest);

    /**
     * Retrieves all shipments with optional filtering
     * @param orderId Filter by order ID
     * @param status Filter by shipment status
     * @param carrierId Filter by carrier ID
     * @param trackingNumber Filter by tracking number
     * @param pageable Pagination parameters
     * @param accountRequest The account request for authorization
     * @return Paginated list of shipment responses
     */
    Page<ShipmentResponse> getAllShipments(Long orderId, ShipmentStatus status, Integer carrierId, 
                                          String trackingNumber, Pageable pageable, 
                                          TAccountRequest accountRequest);

    /**
     * Retrieves all shipments for a specific order
     * @param orderId The order ID
     * @param accountRequest The account request for authorization
     * @return List of shipment responses
     */
    List<ShipmentResponse> getShipmentsByOrderId(Long orderId, TAccountRequest accountRequest);

    /**
     * Tracks a shipment using its tracking number
     * @param trackingNumber The tracking number
     * @return The shipment response with tracking information
     */
    ShipmentResponse trackShipment(String trackingNumber);

    /**
     * Generates a shipping label for a shipment
     * @param id The shipment ID
     * @param accountRequest The account request for authorization
     * @return The shipment response with label URL
     */
    ShipmentResponse generateShippingLabel(Long id, TAccountRequest accountRequest);

    /**
     * Marks a shipment as shipped
     * @param id The shipment ID
     * @param accountRequest The account request for authorization
     * @return The updated shipment response
     */
    ShipmentResponse markShipmentAsShipped(Long id, TAccountRequest accountRequest);

    /**
     * Marks a shipment as delivered
     * @param id The shipment ID
     * @param signature The delivery signature (optional)
     * @param accountRequest The account request for authorization
     * @return The updated shipment response
     */
    ShipmentResponse markShipmentAsDelivered(Long id, String signature, TAccountRequest accountRequest);

    /**
     * Retrieves all tracking events for a shipment
     * @param id The shipment ID
     * @param accountRequest The account request for authorization
     * @return List of tracking event responses
     */
    List<ShipmentResponse.TrackingEventResponse> getShipmentTrackingEvents(Long id, TAccountRequest accountRequest);
} 
