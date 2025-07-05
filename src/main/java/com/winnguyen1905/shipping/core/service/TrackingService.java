package com.winnguyen1905.shipping.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.winnguyen1905.shipping.core.model.request.CreateTrackingEventRequest;
import com.winnguyen1905.shipping.core.model.response.TrackingEventResponse;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import java.util.List;

public interface TrackingService {

    /**
     * Creates a new tracking event for a shipment
     * @param request The tracking event creation request
     * @param accountRequest The account request for authorization
     * @return The created tracking event response
     */
    TrackingEventResponse createTrackingEvent(CreateTrackingEventRequest request, TAccountRequest accountRequest);

    /**
     * Retrieves a tracking event by its ID
     * @param id The tracking event ID
     * @param accountRequest The account request for authorization
     * @return The tracking event response
     */
    TrackingEventResponse getTrackingEventById(Long id, TAccountRequest accountRequest);

    /**
     * Retrieves tracking events with optional filtering
     * @param shipmentId Filter by shipment ID
     * @param trackingNumber Filter by tracking number
     * @param eventType Filter by event type
     * @param pageable Pagination parameters
     * @param accountRequest The account request for authorization
     * @return Paginated list of tracking event responses
     */
    Page<TrackingEventResponse> getTrackingEvents(Long shipmentId, String trackingNumber, String eventType, 
                                                 Pageable pageable, TAccountRequest accountRequest);

    /**
     * Retrieves all tracking events for a specific shipment
     * @param shipmentId The shipment ID
     * @param accountRequest The account request for authorization
     * @return List of tracking event responses
     */
    List<TrackingEventResponse> getTrackingEventsByShipment(Long shipmentId, TAccountRequest accountRequest);

    /**
     * Retrieves all tracking events for a specific tracking number
     * @param trackingNumber The tracking number
     * @return List of tracking event responses
     */
    List<TrackingEventResponse> getTrackingEventsByNumber(String trackingNumber);

    /**
     * Gets the current status of a shipment by tracking number
     * @param trackingNumber The tracking number
     * @return The current tracking status
     */
    TrackingEventResponse.TrackingStatus getTrackingStatus(String trackingNumber);

    /**
     * Refreshes tracking information from the carrier
     * @param trackingNumber The tracking number
     * @param accountRequest The account request for authorization
     * @return List of updated tracking event responses
     */
    List<TrackingEventResponse> refreshTrackingInfo(String trackingNumber, TAccountRequest accountRequest);

    /**
     * Refreshes tracking information for multiple tracking numbers
     * @param trackingNumbers List of tracking numbers
     * @param accountRequest The account request for authorization
     * @return Batch refresh response with results
     */
    TrackingEventResponse.BatchRefreshResponse batchRefreshTrackingInfo(List<String> trackingNumbers, TAccountRequest accountRequest);
} 
