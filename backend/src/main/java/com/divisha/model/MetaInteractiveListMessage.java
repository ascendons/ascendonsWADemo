package com.divisha.model;

import java.util.List;
import java.util.Map;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetaInteractiveListMessage {

  private String header;
  private String body;
  private String footer;
  private String buttonText;

  private String sectionTitle;
  private List<MetaListRow> rows;

  public Map<String, Object> toMetaPayload(String to) {

    List<Map<String, String>> rowList =
        rows.stream()
            .map(
                r ->
                    Map.of(
                        "id", r.getId(),
                        "title", r.getTitle(),
                        "description", r.getDescription()))
            .toList();

    Map<String, Object> section =
        Map.of(
            "title", sectionTitle,
            "rows", rowList);

    return Map.of(
        "messaging_product",
        "whatsapp",
        "to",
        to,
        "type",
        "interactive",
        "interactive",
        Map.of(
            "type", "list",
            "header", Map.of("type", "text", "text", header),
            "body", Map.of("text", body),
            "footer", Map.of("text", footer),
            "action", Map.of("button", buttonText, "sections", List.of(section))));
  }
}
