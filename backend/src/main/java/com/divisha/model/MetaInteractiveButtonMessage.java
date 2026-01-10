package com.divisha.model;

import java.util.List;
import java.util.Map;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetaInteractiveButtonMessage {

  private String header; // text-only header
  private String body;
  private String footer;

  private List<MetaButtonOption> buttons;

  // Converts to Meta-compliant map structure (used inside MetaMessageSender)
  public Map<String, Object> toMetaPayload(String to) {

    List<Map<String, Object>> buttonList =
        buttons.stream()
            .map(
                b ->
                    Map.of(
                        "type",
                        "reply",
                        "reply",
                        Map.of(
                            "id", b.getId(),
                            "title", b.getTitle())))
            .toList();

    return Map.of(
        "messaging_product",
        "whatsapp",
        "to",
        to,
        "type",
        "interactive",
        "interactive",
        Map.of(
            "type", "button",
            "header", Map.of("type", "text", "text", header),
            "body", Map.of("text", body),
            "footer", Map.of("text", footer),
            "action", Map.of("buttons", buttonList)));
  }
}
