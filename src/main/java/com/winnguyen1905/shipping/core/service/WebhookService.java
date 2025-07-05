package com.winnguyen1905.shipping.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.winnguyen1905.shipping.core.model.request.CreateWebhookRequest;
import com.winnguyen1905.shipping.core.model.response.WebhookResponse;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import java.util.List;

public interface WebhookService {

    /**
     * Creates a new webhook entry
     * @param request The webhook creation request
     * @param accountRequest The account request for authorization
     * @return The created webhook response
     */
    WebhookResponse createWebhook(CreateWebhookRequest request, TAccountRequest accountRequest);

    /**
     * Retrieves a webhook by its ID
     * @param id The webhook ID
     * @param accountRequest The account request for authorization
     * @return The webhook response
     */
    WebhookResponse getWebhookById(Long id, TAccountRequest accountRequest);

    /**
     * Retrieves all webhooks with optional filtering
     * @param shipmentId Filter by shipment ID
     * @param carrierId Filter by carrier ID
     * @param webhookType Filter by webhook type
     * @param processed Filter by processed status
     * @param trackingNumber Filter by tracking number
     * @param pageable Pagination parameters
     * @param accountRequest The account request for authorization
     * @return Paginated list of webhook responses
     */
    Page<WebhookResponse> getAllWebhooks(Long shipmentId, Integer carrierId, String webhookType, 
                                        Boolean processed, String trackingNumber, 
                                        Pageable pageable, TAccountRequest accountRequest);

    /**
     * Retrieves all webhooks for a specific shipment
     * @param shipmentId The shipment ID
     * @param accountRequest The account request for authorization
     * @return List of webhook responses
     */
    List<WebhookResponse> getWebhooksByShipment(Long shipmentId, TAccountRequest accountRequest);

    /**
     * Retrieves all webhooks for a specific carrier
     * @param carrierId The carrier ID
     * @param accountRequest The account request for authorization
     * @return List of webhook responses
     */
    List<WebhookResponse> getWebhooksByCarrier(Integer carrierId, TAccountRequest accountRequest);

    /**
     * Retrieves all unprocessed webhooks
     * @param accountRequest The account request for authorization
     * @return List of unprocessed webhook responses
     */
    List<WebhookResponse> getUnprocessedWebhooks(TAccountRequest accountRequest);

    /**
     * Processes a webhook by its ID
     * @param id The webhook ID
     * @param accountRequest The account request for authorization
     * @return The processed webhook response
     */
    WebhookResponse processWebhook(Long id, TAccountRequest accountRequest);

    /**
     * Processes multiple webhooks in batch
     * @param webhookIds List of webhook IDs to process
     * @param accountRequest The account request for authorization
     * @return Batch processing response
     */
    WebhookResponse.BatchProcessResponse processBatchWebhooks(List<Long> webhookIds, TAccountRequest accountRequest);

    /**
     * Processes all unprocessed webhooks
     * @param accountRequest The account request for authorization
     * @return Batch processing response
     */
    WebhookResponse.BatchProcessResponse processAllUnprocessedWebhooks(TAccountRequest accountRequest);

    /**
     * Retries processing a failed webhook
     * @param id The webhook ID
     * @param accountRequest The account request for authorization
     * @return The retried webhook response
     */
    WebhookResponse retryWebhookProcessing(Long id, TAccountRequest accountRequest);

    /**
     * Retrieves webhook statistics
     * @param accountRequest The account request for authorization
     * @return Webhook statistics response
     */
    WebhookResponse.WebhookStatistics getWebhookStatistics(TAccountRequest accountRequest);

    /**
     * Retrieves webhook statistics for a specific carrier
     * @param carrierId The carrier ID
     * @param accountRequest The account request for authorization
     * @return Webhook statistics response
     */
    WebhookResponse.WebhookStatistics getWebhookStatisticsByCarrier(Integer carrierId, TAccountRequest accountRequest);
} 
