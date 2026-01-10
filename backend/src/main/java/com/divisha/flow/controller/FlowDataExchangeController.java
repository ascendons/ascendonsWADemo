package com.divisha.flow.controller;

import com.divisha.configuration.MetaV3Crypto;
import com.divisha.flow.dto.FlowDataExchangeRequest;
import com.divisha.flow.dto.FlowDataExchangeResponse;
import com.divisha.flow.service.AppointmentFlowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to handle WhatsApp Flow data_exchange requests. This endpoint receives encrypted
 * requests from WhatsApp Flow screens when they use data_exchange action to fetch dynamic data.
 */
@Slf4j
@RestController
@RequestMapping("/meta/flow/data-exchange")
@RequiredArgsConstructor
public class FlowDataExchangeController {

  private final AppointmentFlowService appointmentFlowService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  // Hardcoded private key (base64 encoded, without BEGIN/END markers and newlines)
  @Value("${meta.flow.private.key:}")
  private String hardcodedPrivateKey;

  /**
   * Handle data_exchange requests from WhatsApp Flow. The request comes encrypted, we decrypt it,
   * process it, and return encrypted response.
   */
  @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> handleDataExchange(@RequestBody Map<String, Object> payload) {

    log.info("📥 Received data_exchange request");

    try {
      // Extract encryption parameters
      String encryptedFlowData = (String) payload.get("encrypted_flow_data");
      String encryptedAesKey = (String) payload.get("encrypted_aes_key");
      String initialVector = (String) payload.get("initial_vector");

      if (encryptedFlowData == null || encryptedAesKey == null || initialVector == null) {
        log.error("❌ Missing encryption parameters in request");
        return ResponseEntity.badRequest().body("Invalid request format");
      }

      // Decrypt the request
      PrivateKey privateKey = loadPrivateKey();
      byte[] aesKey = MetaV3Crypto.rsaDecryptAesKey(encryptedAesKey, privateKey);
      byte[] iv = Base64.getDecoder().decode(initialVector);

      String decryptedJson = MetaV3Crypto.decryptRequestPayload(aesKey, encryptedFlowData, iv);
      log.info("📄 Decrypted request: {}", decryptedJson);

      // Parse the decrypted request
      FlowDataExchangeRequest request =
          objectMapper.readValue(decryptedJson, FlowDataExchangeRequest.class);

      log.info(
          "📋 Parsed request - Screen: {}, Action: {}", request.getScreen(), request.getAction());
      log.info("📋 Request data: {}", request.getData());

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

      return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(encryptedResponse);

    } catch (Exception e) {
      log.error("❌ Error processing data_exchange request: {}", e.getMessage(), e);

      try {
        // Try to return an encrypted error response
        String encryptedAesKey = (String) payload.get("encrypted_aes_key");
        String initialVector = (String) payload.get("initial_vector");

        if (encryptedAesKey != null && initialVector != null) {
          PrivateKey privateKey = loadPrivateKey();
          byte[] aesKey = MetaV3Crypto.rsaDecryptAesKey(encryptedAesKey, privateKey);
          byte[] iv = Base64.getDecoder().decode(initialVector);

          FlowDataExchangeResponse errorResponse =
              FlowDataExchangeResponse.builder()
                  .version("3.0")
                  .errorMessage("An error occurred processing your request. Please try again.")
                  .build();

          String errorJson = objectMapper.writeValueAsString(errorResponse);
          String encryptedError = MetaV3Crypto.encryptResponsePayload(aesKey, errorJson, iv);

          return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(encryptedError);
        }
      } catch (Exception encryptionError) {
        log.error("❌ Failed to encrypt error response: {}", encryptionError.getMessage());
      }

      return ResponseEntity.internalServerError().body("Failed to process request");
    }
  }

  /** Load private key from configuration (hardcoded) for RSA decryption. */
  private PrivateKey loadPrivateKey() throws Exception {
    // Use hardcoded private key from application.properties
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

  /** Health check endpoint for testing. */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of("status", "healthy", "service", "flow-data-exchange"));
  }
}
