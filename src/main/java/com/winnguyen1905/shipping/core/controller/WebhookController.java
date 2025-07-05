package com.winnguyen1905.shipping.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.winnguyen1905.shipping.core.model.request.CreateWebhookRequest;
import com.winnguyen1905.shipping.core.model.response.WebhookResponse;
import com.winnguyen1905.shipping.core.service.WebhookService;
import com.winnguyen1905.shipping.secure.AccountRequest;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhook Management", description = "APIs for managing carrier webhooks")
public class WebhookController {

    @Autowired
    private WebhookService webhookService;

    @PostMapping
    @Operation(summary = "Create a new webhook", 
               description = "Creates a new webhook entry for carrier integration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Webhook created successfully",
                    content = @Content(schema = @Schema(implementation = WebhookResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<WebhookResponse> createWebhook(
            @Valid @RequestBody CreateWebhookRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        WebhookResponse response = webhookService.createWebhook(request, accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get webhook by ID", 
               description = "Retrieves a specific webhook by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook found",
                    content = @Content(schema = @Schema(implementation = WebhookResponse.class))),
        @ApiResponse(responseCode = "404", description = "Webhook not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<WebhookResponse> getWebhook(
            @Parameter(description = "Webhook ID") @PathVariable Long id,
            @AccountRequest TAccountRequest accountRequest) {
        WebhookResponse response = webhookService.getWebhookById(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all webhooks", 
               description = "Retrieves a paginated list of webhooks with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhooks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<WebhookResponse>> getAllWebhooks(
            @Parameter(description = "Shipment ID filter") @RequestParam(required = false) Long shipmentId,
            @Parameter(description = "Carrier ID filter") @RequestParam(required = false) Integer carrierId,
            @Parameter(description = "Webhook type filter") @RequestParam(required = false) String webhookType,
            @Parameter(description = "Processed status filter") @RequestParam(required = false) Boolean processed,
            @Parameter(description = "Tracking number filter") @RequestParam(required = false) String trackingNumber,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AccountRequest TAccountRequest accountRequest) {
        Page<WebhookResponse> response = webhookService.getAllWebhooks(
            shipmentId, carrierId, webhookType, processed, trackingNumber, pageable, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shipment/{shipmentId}")
    @Operation(summary = "Get webhooks by shipment ID", 
               description = "Retrieves all webhooks for a specific shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhooks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<WebhookResponse>> getWebhooksByShipment(
            @Parameter(description = "Shipment ID") @PathVariable Long shipmentId,
            @AccountRequest TAccountRequest accountRequest) {
        List<WebhookResponse> response = webhookService.getWebhooksByShipment(shipmentId, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/carrier/{carrierId}")
    @Operation(summary = "Get webhooks by carrier ID", 
               description = "Retrieves all webhooks for a specific carrier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhooks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<WebhookResponse>> getWebhooksByCarrier(
            @Parameter(description = "Carrier ID") @PathVariable Integer carrierId,
            @AccountRequest TAccountRequest accountRequest) {
        List<WebhookResponse> response = webhookService.getWebhooksByCarrier(carrierId, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unprocessed")
    @Operation(summary = "Get unprocessed webhooks", 
               description = "Retrieves all unprocessed webhooks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Unprocessed webhooks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<WebhookResponse>> getUnprocessedWebhooks(
            @AccountRequest TAccountRequest accountRequest) {
        List<WebhookResponse> response = webhookService.getUnprocessedWebhooks(accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Process webhook", 
               description = "Processes a webhook by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully",
                    content = @Content(schema = @Schema(implementation = WebhookResponse.class))),
        @ApiResponse(responseCode = "404", description = "Webhook not found"),
        @ApiResponse(responseCode = "400", description = "Webhook cannot be processed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<WebhookResponse> processWebhook(
            @Parameter(description = "Webhook ID") @PathVariable Long id,
            @AccountRequest TAccountRequest accountRequest) {
        WebhookResponse response = webhookService.processWebhook(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch-process")
    @Operation(summary = "Process multiple webhooks", 
               description = "Processes multiple webhooks in batch")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch processing completed"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<WebhookResponse.BatchProcessResponse> processBatchWebhooks(
            @Parameter(description = "List of webhook IDs") @RequestBody List<Long> webhookIds,
            @AccountRequest TAccountRequest accountRequest) {
        WebhookResponse.BatchProcessResponse response = webhookService.processBatchWebhooks(webhookIds, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process-all-unprocessed")
    @Operation(summary = "Process all unprocessed webhooks", 
               description = "Processes all unprocessed webhooks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All unprocessed webhooks processed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<WebhookResponse.BatchProcessResponse> processAllUnprocessedWebhooks(
            @AccountRequest TAccountRequest accountRequest) {
        WebhookResponse.BatchProcessResponse response = webhookService.processAllUnprocessedWebhooks(accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry webhook processing", 
               description = "Retries processing a failed webhook")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook retry successful",
                    content = @Content(schema = @Schema(implementation = WebhookResponse.class))),
        @ApiResponse(responseCode = "404", description = "Webhook not found"),
        @ApiResponse(responseCode = "400", description = "Webhook cannot be retried"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<WebhookResponse> retryWebhookProcessing(
            @Parameter(description = "Webhook ID") @PathVariable Long id,
            @AccountRequest TAccountRequest accountRequest) {
        WebhookResponse response = webhookService.retryWebhookProcessing(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get webhook statistics", 
               description = "Retrieves overall webhook statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<WebhookResponse.WebhookStatistics> getWebhookStatistics(
            @AccountRequest TAccountRequest accountRequest) {
        WebhookResponse.WebhookStatistics response = webhookService.getWebhookStatistics(accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics/carrier/{carrierId}")
    @Operation(summary = "Get webhook statistics by carrier", 
               description = "Retrieves webhook statistics for a specific carrier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Carrier statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<WebhookResponse.WebhookStatistics> getWebhookStatisticsByCarrier(
            @Parameter(description = "Carrier ID") @PathVariable Integer carrierId,
            @AccountRequest TAccountRequest accountRequest) {
        WebhookResponse.WebhookStatistics response = webhookService.getWebhookStatisticsByCarrier(carrierId, accountRequest);
        return ResponseEntity.ok(response);
    }

    // Public endpoint for carriers to send webhooks (no authentication required)
    @PostMapping("/receive/{carrierCode}")
    @Operation(summary = "Receive webhook from carrier", 
               description = "Receives webhook data from carriers (public endpoint)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook received successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid webhook data")
    })
    public ResponseEntity<String> receiveWebhook(
            @Parameter(description = "Carrier code") @PathVariable String carrierCode,
            @RequestBody String webhookData,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        // This endpoint would handle incoming webhooks from carriers
        // Implementation would validate the webhook signature and process the data
        return ResponseEntity.ok("Webhook received successfully");
    }
} 
