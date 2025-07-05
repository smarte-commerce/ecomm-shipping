package com.winnguyen1905.shipping.core.model.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWebhookRequest {

  private Long shipmentId;

  @NotNull(message = "Carrier ID is required")
  private Integer carrierId;

  @NotBlank(message = "Webhook type is required")
  private String webhookType;

  private String trackingNumber;

  @NotBlank(message = "Webhook data is required")
  private String webhookData;
}
