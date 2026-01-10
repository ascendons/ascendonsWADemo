package com.divisha.controller;

import com.divisha.service.MessageService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/meta/webhook")
@RequiredArgsConstructor
public class MetaWebhookController {

  private final MessageService messageService;

  @Value("${meta.api.webhook.token}")
  private String verifyWebhookTokens;

  // ---------------------------------------------------------------------
  // 1️⃣ VERIFY TOKEN (required by Meta during webhook setup)
  // ---------------------------------------------------------------------
  @GetMapping
  public ResponseEntity<String> verifyWebhook(
      @RequestParam(name = "hub.mode", required = false) String mode,
      @RequestParam(name = "hub.challenge", required = false) String challenge,
      @RequestParam(name = "hub.verify_token", required = false) String verifyToken) {

    log.info("🔍 Incoming Meta Webhook Verification: mode={}, token={}", mode, verifyToken);

    if ("subscribe".equals(mode) && verifyWebhookTokens.equals(verifyToken)) {
      log.info("✅ Meta Webhook Verified Successfully");
      return ResponseEntity.ok(challenge);
    }

    log.warn("❌ Meta Webhook Verification Failed: token mismatch");
    return ResponseEntity.status(403).body("Verification failed");
  }

  // ---------------------------------------------------------------------
  // 2️⃣ HANDLE META MESSAGES / EVENTS (POST)
  // ---------------------------------------------------------------------
  @PostMapping
  public ResponseEntity<Void> receiveMessage(
      @RequestBody Map<String, Object> payload,
      @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

    log.info("📩 Incoming Meta webhook payload: {}", payload);
    try {
      log.info("📩 Full payload JSON: {}", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload));
    } catch (Exception e) {
      log.warn("Could not serialize payload", e);
    }

    // TODO: Optional — verify signature here using App Secret

    messageService.processIncomingMetaMessage(payload);

    return ResponseEntity.ok().build();
  }
}
