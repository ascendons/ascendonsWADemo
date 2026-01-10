package com.divisha.controller;

import com.divisha.configuration.MetaV3Crypto;
import com.divisha.flow.dto.FlowDataExchangeRequest;
import com.divisha.flow.dto.FlowDataExchangeResponse;
import com.divisha.flow.service.AppointmentFlowService;
import com.divisha.model.FlowResponseData;
import com.divisha.service.FlowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/meta/flow/webhook")
@RequiredArgsConstructor
public class FlowWebhookController {

  private final FlowService flowService;
  private final AppointmentFlowService appointmentFlowService;
  private final ObjectMapper objectMapper;

  @Value("${meta.flow.private.key:}")
  private String hardcodedPrivateKey;

  @Value("${meta.api.webhook.token}")
  private String verifyWebhookToken;

  // --------------------------------------------
  // 1️⃣ VERIFY WEBHOOK
  // --------------------------------------------
  @GetMapping
  public ResponseEntity<String> verifyWebhook(
      @RequestParam(name = "hub.mode", required = false) String mode,
      @RequestParam(name = "hub.challenge", required = false) String challenge,
      @RequestParam(name = "hub.verify_token", required = false) String token) {

    log.info("🔍 Webhook verification request - mode: {}, token provided: {}", mode, token != null);

    if ("subscribe".equals(mode) && verifyWebhookToken.equals(token)) {
      log.info("✅ Webhook verification successful");
      return ResponseEntity.ok(challenge);
    }
    log.warn("❌ Webhook verification failed - invalid token or mode");
    return ResponseEntity.status(403).body("Verification failed");
  }

  // --------------------------------------------
  // 0️⃣ HEALTH CHECK
  // --------------------------------------------
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(
        Map.of(
            "status", "healthy",
            "service", "flow-webhook",
            "endpoint", "/meta/flow/webhook"));
  }

  // --------------------------------------------
  // 2️⃣ HANDLE PRODUCTION WEBHOOK EVENTS
  // --------------------------------------------
  @PostMapping
  public ResponseEntity<?> receiveFlowCompletion(@RequestBody Map<String, Object> payload) {

    log.info("📩 Incoming Flow Webhook Payload: {}", payload);

    try {
      String encryptedFlowData = (String) payload.get("encrypted_flow_data");
      String encryptedAesKey = (String) payload.get("encrypted_aes_key");
      String initialVector = (String) payload.get("initial_vector");

      PrivateKey key = loadPrivateKey();
      byte[] aesKey = MetaV3Crypto.rsaDecryptAesKey(encryptedAesKey, key);
      byte[] iv = Base64.getDecoder().decode(initialVector);

      String decryptedJson = MetaV3Crypto.decryptRequestPayload(aesKey, encryptedFlowData, iv);
      log.info("📄 Decrypted JSON: {}", decryptedJson);

      Map<String, Object> map = objectMapper.readValue(decryptedJson, Map.class);
      String action = (String) map.get("action");
      if (action.equals("ping")) {
        String response =
            MetaV3Crypto.encryptResponsePayload(
                aesKey,
                objectMapper.writeValueAsString(Map.of("data", Map.of("status", "active"))),
                iv);
        return ResponseEntity.ok()
            .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
            .body(response);
      }

      String flowToken = (String) payload.get("flow_token");
      log.info("🎯 Action type: {}", action);
      FlowDataExchangeRequest flowDataExchangeRequest =
          objectMapper.readValue(decryptedJson, FlowDataExchangeRequest.class);
      String requestFlowToken = flowDataExchangeRequest.getFlowToken();
      FlowInfo info = parseFlowString(requestFlowToken);

      if ("data_exchange".equals(action)) {
        log.info("📊 Processing data_exchange action during flow");
        return handleDataExchangeInWebhook(decryptedJson, aesKey, iv);
      } else {
        log.info("✅ Processing flow completion");
        FlowResponseData decrypted = objectMapper.readValue(decryptedJson, FlowResponseData.class);
        decrypted.setFlowToken(flowToken);

        log.info(
            "✅ Decrypted flow data - FlowId: {}, Action: {}, Data keys: {}",
            decrypted.getFlowId(),
            decrypted.getAction(),
            decrypted.getData().keySet());

        flowService.processFlowCompletion(info.phoneNumber(), decrypted);
        return ResponseEntity.ok(Map.of("status", "success"));
      }

    } catch (Exception e) {
      log.error("❌ Error: {}", e.getMessage(), e);
      return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  // --------------------------------------------
  // 2.5️⃣ HANDLE DATA_EXCHANGE IN WEBHOOK
  // --------------------------------------------
  /**
   * Handle data_exchange actions that come through the webhook (during flow interaction) This is
   * the same logic as FlowDataExchangeController but accessed via webhook
   */
  private ResponseEntity<?> handleDataExchangeInWebhook(
      String decryptedJson, byte[] aesKey, byte[] iv) {
    try {
      log.info("📊 Processing data_exchange in webhook");

      // Parse the decrypted request
      FlowDataExchangeRequest request =
          objectMapper.readValue(decryptedJson, FlowDataExchangeRequest.class);

      log.info("📋 Parsed request - Screen: {}, Data: {}", request.getScreen(), request.getData());

      // Process the request and get response
      FlowDataExchangeResponse response = appointmentFlowService.handleDataExchange(request);

      // Convert response to JSON
      String responseJson = objectMapper.writeValueAsString(response);
      log.info(
          "📤 Response screen: {}, data keys: {}",
          response.getScreen(),
          response.getData().keySet());
      log.info("📤 Full response JSON: {}", responseJson);

      // Encrypt the response
      String encryptedResponse = MetaV3Crypto.encryptResponsePayload(aesKey, responseJson, iv);

      log.info("✅ Data exchange processed and encrypted successfully");

      // Return encrypted response as plain text (WhatsApp Flow format)
      return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(encryptedResponse);

    } catch (Exception e) {
      log.error("❌ Error processing data_exchange in webhook: {}", e.getMessage(), e);

      try {
        // Return encrypted error response
        FlowDataExchangeResponse errorResponse =
            FlowDataExchangeResponse.builder()
                .version("3.0")
                .errorMessage("An error occurred processing your request. Please try again.")
                .build();

        String errorJson = objectMapper.writeValueAsString(errorResponse);
        String encryptedError = MetaV3Crypto.encryptResponsePayload(aesKey, errorJson, iv);

        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(encryptedError);
      } catch (Exception encryptionError) {
        log.error("❌ Failed to encrypt error response: {}", encryptionError.getMessage());
        return ResponseEntity.internalServerError()
            .body(Map.of("status", "error", "message", "Failed to process request"));
      }
    }
  }

  // --------------------------------------------
  // 3️⃣ HANDLE DIRECT TEST PAYLOADS
  // --------------------------------------------
  private ResponseEntity<?> handleDirectEncryptedPayload(Map<String, Object> payload) {
    try {
      String encryptedFlowData = (String) payload.get("encrypted_flow_data");
      String encryptedAesKey = (String) payload.get("encrypted_aes_key");
      String initialVector = (String) payload.get("initial_vector");

      PrivateKey key = loadPrivateKey();
      byte[] aesKey = MetaV3Crypto.rsaDecryptAesKey(encryptedAesKey, key);
      byte[] iv = Base64.getDecoder().decode(initialVector);

      String json = MetaV3Crypto.decryptRequestPayload(aesKey, encryptedFlowData, iv);
      log.info("📄 Decrypted JSON: {}", json);

      Map<String, Object> map = objectMapper.readValue(json, Map.class);
      String action = (String) map.get("action");
      if (action.equals("ping")) {
        String response =
            MetaV3Crypto.encryptResponsePayload(
                aesKey,
                objectMapper.writeValueAsString(Map.of("data", Map.of("status", "active"))),
                iv);
        return ResponseEntity.ok()
            .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
            .body(response);
      }
      return null;

    } catch (Exception e) {
      log.error("❌ Decryption error: {}", e.getMessage());
      return ResponseEntity.ok("");
    }
  }

  // --------------------------------------------
  // 4️⃣ DECRYPT FULL FLOW DATA (AES + RSA)
  // --------------------------------------------
  private FlowResponseData decryptFlowData(
      String encryptedFlowData, String encryptedAesKey, String initialVector, String flowToken)
      throws Exception {

    PrivateKey key = loadPrivateKey();

    byte[] aesKey = MetaV3Crypto.rsaDecryptAesKey(encryptedAesKey, key);
    byte[] iv = Base64.getDecoder().decode(initialVector);

    String decryptedJson = MetaV3Crypto.decryptRequestPayload(aesKey, encryptedFlowData, iv);
    FlowResponseData data = objectMapper.readValue(decryptedJson, FlowResponseData.class);
    data.setFlowToken(flowToken);

    return data;
  }

  // --------------------------------------------
  // 5️⃣ LOAD PRIVATE KEY (HARDCODED)
  // --------------------------------------------
  private PrivateKey loadPrivateKey() throws Exception {
    if (hardcodedPrivateKey == null || hardcodedPrivateKey.trim().isEmpty()) {
      throw new IllegalStateException(
          "Private key not configured. Please set 'meta.flow.private.key' in application.properties");
    }

    // The key should already be base64 encoded without BEGIN/END markers
    String sanitized = hardcodedPrivateKey.replaceAll("\\s", "");

    byte[] keyBytes = Base64.getDecoder().decode(sanitized);

    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
    return KeyFactory.getInstance("RSA").generatePrivate(spec);
  }

  public static FlowInfo parseFlowString(String input) {
    if (input.isEmpty()) {
      return new FlowInfo("", "");
    }
    String regex = "^FLOW_(.+?)_(.+)$";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(input);

    if (matcher.find()) {
      return new FlowInfo(
          matcher.group(1), // phoneNumber
          matcher.group(2) // patientId
          );
    } else {
      throw new IllegalArgumentException("Invalid FLOW string format: " + input);
    }
  }
}
