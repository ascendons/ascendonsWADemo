package com.divisha.flow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for WhatsApp Flow data_exchange endpoint. This is the decrypted payload received
 * when a Flow screen uses data_exchange action.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowDataExchangeRequest {

  @JsonProperty("version")
  private String version;

  @JsonProperty("action")
  private String action; // "data_exchange"

  @JsonProperty("screen")
  private String screen; // Current screen ID

  @JsonProperty("data")
  private Map<String, Object> data; // Current form field values

  @JsonProperty("flow_token")
  private String flowToken; // Unique token for this flow session
}
