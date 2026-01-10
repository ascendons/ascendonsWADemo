package com.divisha.flow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload for WhatsApp Flow data_exchange endpoint. This response will be encrypted and
 * sent back to WhatsApp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowDataExchangeResponse {

  @JsonProperty("version")
  private String version; // Should match request version

  @JsonProperty("screen")
  private String screen; // Screen to navigate to (or same screen)

  @JsonProperty("data")
  private Map<String, Object> data; // Data to populate in the screen

  @JsonProperty("error_message")
  private String errorMessage; // Optional error message to display
}
