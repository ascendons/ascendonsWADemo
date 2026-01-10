package com.divisha.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlowResponseData {

  @JsonProperty("flow_token")
  private String flowToken;

  @JsonProperty("action")
  private String action; // "complete", "back", "navigate"

  @JsonProperty("screen")
  private String screen; // Screen ID where flow completed

  @JsonProperty("data")
  private Map<String, Object> data; // Form field values submitted by user

  @JsonProperty("version")
  private String version; // Flow version

  @JsonProperty("flow_id")
  private String flowId; // WhatsApp Flow ID
}
