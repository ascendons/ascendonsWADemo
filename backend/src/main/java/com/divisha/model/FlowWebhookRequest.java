package com.divisha.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlowWebhookRequest {

  @JsonProperty("version")
  private String version; // Flow version (should be "3.0")

  @JsonProperty("flow_token")
  private String flowToken; // Token sent with the flow

  @JsonProperty("encrypted_flow_data")
  private String encryptedFlowData; // Encrypted flow response data

  @JsonProperty("encrypted_aes_key")
  private String encryptedAesKey; // RSA-encrypted AES key

  @JsonProperty("initial_vector")
  private String initialVector; // AES-GCM IV

  // Additional webhook metadata from Meta
  @JsonProperty("messaging_product")
  private String messagingProduct; // "whatsapp"

  @JsonProperty("from")
  private String from; // User phone number

  @JsonProperty("timestamp")
  private String timestamp;
}
