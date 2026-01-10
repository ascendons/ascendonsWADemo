package com.divisha.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaTemplateMessage {

  private String templateName;
  private String languageCode; // e.g., "en", "en_US"

  @Builder.Default private List<TemplateComponent> components = new ArrayList<>();

  /**
   * Add a header component parameter
   *
   * @param type "text", "image", "document", "video"
   * @param value The value (text or media URL)
   */
  public MetaTemplateMessage addHeaderParameter(String type, String value) {
    TemplateComponent header = findOrCreateComponent("header");
    header.addParameter(type, value);
    return this;
  }

  /**
   * Add body text parameters in order
   *
   * @param values List of parameter values
   */
  public MetaTemplateMessage addBodyParameters(String... values) {
    TemplateComponent body = findOrCreateComponent("body");
    for (String value : values) {
      body.addParameter("text", value);
    }
    return this;
  }

  /**
   * Add a button parameter (for dynamic buttons)
   *
   * @param index Button index (0-based)
   * @param type "text" or "payload"
   * @param value The button value
   */
  public MetaTemplateMessage addButtonParameter(int index, String type, String value) {
    TemplateComponent button = new TemplateComponent("button");
    button.setSubType("quick_reply"); // or "url"
    button.setIndex(index);
    button.addParameter(type, value);
    components.add(button);
    return this;
  }

  private TemplateComponent findOrCreateComponent(String type) {
    return components.stream()
        .filter(c -> c.getType().equals(type))
        .findFirst()
        .orElseGet(
            () -> {
              TemplateComponent newComp = new TemplateComponent(type);
              components.add(newComp);
              return newComp;
            });
  }

  /**
   * Convert to Meta API payload format
   *
   * @param whatsapp Recipient phone number (with country code, no +)
   * @return Map ready to send to Meta API
   */
  public Map<String, Object> toMetaPayload(String whatsapp) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("messaging_product", "whatsapp");
    payload.put("recipient_type", "individual");
    payload.put("to", whatsapp);
    payload.put("type", "template");

    Map<String, Object> template = new HashMap<>();
    template.put("name", templateName);

    Map<String, String> language = new HashMap<>();
    language.put("code", languageCode != null ? languageCode : "en");
    template.put("language", language);

    if (!components.isEmpty()) {
      List<Map<String, Object>> componentsList = new ArrayList<>();
      for (TemplateComponent comp : components) {
        componentsList.add(comp.toMap());
      }
      template.put("components", componentsList);
    }

    payload.put("template", template);
    return payload;
  }

  @Data
  public static class TemplateComponent {
    private String type; // header, body, button
    private String subType; // for buttons: quick_reply, url
    private Integer index; // for buttons
    private List<Map<String, Object>> parameters;

    public TemplateComponent() {
      this.parameters = new ArrayList<>();
    }

    public TemplateComponent(String type) {
      this.type = type;
      this.parameters = new ArrayList<>();
    }

    public TemplateComponent(
        String type, String subType, Integer index, List<Map<String, Object>> parameters) {
      this.type = type;
      this.subType = subType;
      this.index = index;
      this.parameters = parameters != null ? parameters : new ArrayList<>();
    }

    public void addParameter(String type, Object value) {
      Map<String, Object> param = new HashMap<>();
      param.put("type", type);

      if ("text".equals(type)) {
        param.put("text", value);
      } else if ("image".equals(type) || "document".equals(type) || "video".equals(type)) {
        Map<String, String> media = new HashMap<>();
        media.put("link", value.toString());
        param.put(type, media);
      } else {
        param.put("payload", value);
      }

      parameters.add(param);
    }

    public Map<String, Object> toMap() {
      Map<String, Object> map = new HashMap<>();
      map.put("type", type);

      if (subType != null) {
        map.put("sub_type", subType);
      }

      if (index != null) {
        map.put("index", index);
      }

      if (!parameters.isEmpty()) {
        map.put("parameters", parameters);
      }

      return map;
    }
  }
}
