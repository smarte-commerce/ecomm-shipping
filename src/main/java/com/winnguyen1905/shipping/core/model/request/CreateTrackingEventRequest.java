package com.winnguyen1905.shipping.core.model.request;

import java.time.Instant;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTrackingEventRequest {

    @NotNull(message = "Shipment ID is required")
    @JsonProperty("shipment_id")
    private Long shipmentId;

    @NotBlank(message = "Tracking number is required")
    @JsonProperty("tracking_number")
    private String trackingNumber;

    @NotBlank(message = "Event type is required")
    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("event_description")
    private String eventDescription;

    @JsonProperty("event_location")
    private String eventLocation;

    @NotNull(message = "Event timestamp is required")
    @JsonProperty("event_timestamp")
    private Instant eventTimestamp;

    @JsonProperty("carrier_event_code")
    private String carrierEventCode;
} 
