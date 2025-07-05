package com.winnguyen1905.shipping.core.model.request;

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
public class CreateWebhookRequest {

  @JsonProperty("shipment_id")
  private Long shipmentId;

  @NotNull(message = "Carrier ID is required")
  @JsonProperty("carrier_id")
  private Integer carrierId;

  @NotBlank(message = "Webhook type is required")
  @JsonProperty("webhook_type")
  private String webhookType;

  @JsonProperty("tracking_number")
  private String trackingNumber;

  @NotBlank(message = "Webhook data is required")
  @JsonProperty("webhook_data")
  private String webhookData;
}
