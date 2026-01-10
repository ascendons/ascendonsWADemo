package com.divisha.model;

import java.util.HashMap;
import java.util.Map;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetaFlowMessage {

  private String header;
  private String body;
  private String footer;

  private String flowId;
  private String flowCta;
  private String flowAction;
  private String screenId;

  private Map<String, Object> flowActionPayload;

  public Map<String, Object> toMetaPayload(String to) {

    Map<String, Object> actionMap = new HashMap<>();
    actionMap.put("name", "flow");

    Map<String, Object> parametersMap = new HashMap<>();
    parametersMap.put("flow_message_version", "3");
    parametersMap.put("flow_token", generateFlowToken(to));
    parametersMap.put("flow_id", flowId);
    parametersMap.put("flow_cta", flowCta);
    parametersMap.put("flow_action", flowAction != null ? flowAction : "navigate");

    Map<String, Object> flowActionPayloadMap = new HashMap<>();
    if (screenId != null && !screenId.isEmpty()) {
      flowActionPayloadMap.put("screen", screenId);
    }
    if (flowActionPayload != null && !flowActionPayload.isEmpty()) {
      flowActionPayloadMap.put("data", flowActionPayload);
    }
    if (!flowActionPayloadMap.isEmpty()) {
      parametersMap.put("flow_action_payload", flowActionPayloadMap);
    }

    actionMap.put("parameters", parametersMap);

    Map<String, Object> interactiveMap = new HashMap<>();
    interactiveMap.put("type", "flow");

    if (header != null && !header.isEmpty()) {
      interactiveMap.put("header", Map.of("type", "text", "text", header));
    }

    interactiveMap.put("body", Map.of("text", body));
    if (footer != null && !footer.isEmpty()) {
      interactiveMap.put("footer", Map.of("text", footer));
    }
    interactiveMap.put("action", actionMap);
    return Map.of(
        "messaging_product", "whatsapp",
        "recipient_type", "individual",
        "to", to,
        "type", "interactive",
        "interactive", interactiveMap);
  }

  // Generate unique flow token for this conversation
  private String generateFlowToken(String phoneNumber) {
    return "FLOW_" + phoneNumber + "_" + flowActionPayload.get("patient_id");
  }
}
