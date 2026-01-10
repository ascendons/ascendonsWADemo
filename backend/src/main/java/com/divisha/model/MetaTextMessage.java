package com.divisha.model;

import java.util.Map;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetaTextMessage {
  private String text;

  public Map<String, Object> toMetaPayload(String to) {
    return Map.of(
        "messaging_product", "whatsapp", "to", to, "type", "text", "text", Map.of("body", text));
  }
}
