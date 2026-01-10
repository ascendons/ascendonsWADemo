package com.divisha.service;

import com.divisha.model.MetaFlowMessage;
import com.divisha.model.MetaInteractiveButtonMessage;
import com.divisha.model.MetaInteractiveListMessage;
import com.divisha.model.MetaTemplateMessage;
import com.divisha.model.MetaTextMessage;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetaMessageSender {

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${meta.api.url}")
  private String apiUrl;

  @Value("${meta.api.token}")
  private String accessToken;

  @Value("${meta.phone.number.id}")
  private String phoneNumberId;

  private String endpoint() {
    return apiUrl + "/" + phoneNumberId + "/messages";
  }

  private HttpHeaders headers() {
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth(accessToken);
    h.setContentType(MediaType.APPLICATION_JSON);
    return h;
  }

  private void post(Map<String, Object> body) {
    try {
      HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers());
      ResponseEntity<String> response =
          restTemplate.exchange(endpoint(), HttpMethod.POST, request, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("✅ META call success: {}", response.getBody());
      } else {
        log.error(
            "❌ META call failed: status={}, response={}",
            response.getStatusCode(),
            response.getBody());
      }

    } catch (Exception e) {
      log.error("🚨 META call error: {}", e.getMessage(), e);
    }
  }

  // ----------------------------------------------------
  // SEND TEXT MESSAGE
  // ----------------------------------------------------

  public void sendText(String whatsapp, MetaTextMessage msg) {
    Map<String, Object> body = msg.toMetaPayload(whatsapp);
    post(body);
  }

  // Convenience method (if you want plain text)
  public void text(String whatsapp, String text) {
    sendText(whatsapp, new MetaTextMessage(text));
  }

  // ----------------------------------------------------
  // SEND INTERACTIVE BUTTON MESSAGE
  // ----------------------------------------------------

  public void sendButtons(String whatsapp, MetaInteractiveButtonMessage msg) {
    Map<String, Object> payload = msg.toMetaPayload(whatsapp);
    post(payload);
  }

  // ----------------------------------------------------
  // SEND INTERACTIVE LIST MESSAGE
  // ----------------------------------------------------

  public void sendList(String whatsapp, MetaInteractiveListMessage msg) {
    Map<String, Object> payload = msg.toMetaPayload(whatsapp);
    post(payload);
  }

  // ----------------------------------------------------
  // SEND FLOW MESSAGE
  // ----------------------------------------------------

  public void sendFlow(String whatsapp, MetaFlowMessage msg) {
    log.info(
        "📤 Sending Flow message to {}: flowId={}, cta={}",
        whatsapp,
        msg.getFlowId(),
        msg.getFlowCta());
    Map<String, Object> payload = msg.toMetaPayload(whatsapp);
    post(payload);
  }

  // ----------------------------------------------------
  // SEND TEMPLATE MESSAGE
  // ----------------------------------------------------

  public void sendTemplate(String whatsapp, MetaTemplateMessage msg) {
    log.info("📤 Sending Template message to {}: template={}", whatsapp, msg.getTemplateName());
    Map<String, Object> payload = msg.toMetaPayload(whatsapp);
    post(payload);
  }

  public static void main(String[] args) {
    MetaMessageSender metaMessageSender = new MetaMessageSender();
    metaMessageSender.text("919771186585", "why i'm sending this text on this number");
  }
}
