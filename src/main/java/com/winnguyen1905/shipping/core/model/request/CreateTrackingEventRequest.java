package com.winnguyen1905.shipping.core.model.request;

import java.time.Instant;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTrackingEventRequest {

    @NotNull(message = "Shipment ID is required")
    private Long shipmentId;

    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;

    @NotBlank(message = "Event type is required")
    private String eventType;

    private String eventDescription;

    private String eventLocation;

    @NotNull(message = "Event timestamp is required")
    private Instant eventTimestamp;

    private String carrierEventCode;
}
