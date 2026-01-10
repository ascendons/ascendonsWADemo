package com.divisha.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for managing WhatsApp Message Templates via Meta Graph API
 *
 * <p>This service allows you to create, retrieve, and delete message templates programmatically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppTemplateService {

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${meta.api.url}")
  private String apiUrl;

  @Value("${meta.api.token}")
  private String accessToken;

  @Value("${meta.business.account.id}")
  private String businessAccountId;

  private HttpHeaders headers() {
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth(accessToken);
    h.setContentType(MediaType.APPLICATION_JSON);
    return h;
  }

  /**
   * Create a WhatsApp message template
   *
   * @param templateName Unique template name (lowercase, underscores only)
   * @param category Template category (MARKETING, UTILITY, AUTHENTICATION)
   * @param language Language code (e.g., "en", "en_US")
   * @param components List of template components (header, body, footer, buttons)
   * @return API response
   */
  public String createTemplate(
      String templateName, String category, String language, List<Map<String, Object>> components) {

    String url = apiUrl + "/" + businessAccountId + "/message_templates";

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("name", templateName);
    requestBody.put("category", category);
    requestBody.put("language", language);
    requestBody.put("components", components);

    try {
      HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers());
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.POST, request, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("✅ Template created successfully: {}", templateName);
        log.info("Response: {}", response.getBody());
        return response.getBody();
      } else {
        log.error("❌ Template creation failed: {}", response.getBody());
        return response.getBody();
      }
    } catch (Exception e) {
      log.error("🚨 Template creation error: {}", e.getMessage(), e);
      return "Error: " + e.getMessage();
    }
  }

  /**
   * Create appointment cancellation template
   *
   * @return API response
   */
  public String createAppointmentCancellationTemplate() {
    String templateName = "appointment_cancelled_by_clinic";
    String category = "UTILITY"; // For transactional messages
    String language = "en";

    List<Map<String, Object>> components = new ArrayList<>();

    // Body component with parameters
    Map<String, Object> body = new HashMap<>();
    body.put("type", "BODY");
    body.put(
        "text",
        """
        ❌ *Appointment Cancelled!*
        Your appointment has been cancelled by the clinic.

        🔖 *Booking ID:* {{1}}
        📅 *Date:* {{2}}
        🕒 *Time:* {{3}}
        🩺 *Doctor:* {{4}}

        We apologize for any inconvenience. Feel free to book a new appointment at your convenience.
        Reply anytime to *book again* — just say Hi! 👋
        """);

    // Add examples for the parameters
    List<String> exampleParams = List.of("BID-1234567890", "25/12/2025", "10:00 AM", "Dr. Sharma");
    Map<String, Object> example = new HashMap<>();
    example.put("body_text", List.of(exampleParams));
    body.put("example", example);

    components.add(body);

    return createTemplate(templateName, category, language, components);
  }

  /**
   * Create appointment rescheduled template
   *
   * @return API response
   */
  public String createAppointmentRescheduledTemplate() {
    String templateName = "appointment_rescheduled_by_clinic";
    String category = "UTILITY";
    String language = "en";

    List<Map<String, Object>> components = new ArrayList<>();

    Map<String, Object> body = new HashMap<>();
    body.put("type", "BODY");
    body.put(
        "text",
        """
        ✅ *Appointment Rescheduled!*
        Your appointment has been rescheduled by the clinic.

        🔖 *Booking ID:* {{1}}
        📅 *Date:* {{2}}
        🕒 *Time:* {{3}}
        🩺 *Doctor:* {{4}}
        💰 *Consultation Fee:* {{5}}

        If this time doesn't work for you, feel free to book a new slot.
        See you soon! 👋
        """);

    List<String> exampleParams =
        List.of("BID-1234567890", "25/12/2025", "10:00 AM", "Dr. Sharma", "₹1200");
    Map<String, Object> example = new HashMap<>();
    example.put("body_text", List.of(exampleParams));
    body.put("example", example);

    components.add(body);

    return createTemplate(templateName, category, language, components);
  }

  /**
   * Create appointment booking confirmation template
   *
   * @return API response
   */
  public String createAppointmentConfirmedTemplate() {
    String templateName = "appointment_confirmed";
    String category = "UTILITY";
    String language = "en";

    List<Map<String, Object>> components = new ArrayList<>();

    Map<String, Object> body = new HashMap<>();
    body.put("type", "BODY");
    body.put(
        "text",
        """
        ✅ *Appointment Confirmed!*
        Your appointment has been successfully booked.

        👤 *Patient Name:* {{1}}
        🔖 *Booking ID:* {{2}}
        📅 *Date:* {{3}}
        🕒 *Time:* {{4}}
        🩺 *Doctor:* {{5}}
        📍 *Location:* {{6}}
        💰 *Consultation Fee:* {{7}}

        Please arrive 10 minutes early. Bring any previous medical records if available.

        Thank you for choosing Divisha Arthritis & Medical Center 🙏
        """);

    List<String> exampleParams =
        List.of(
            "Rajesh Kumar",
            "BID-1234567890",
            "25/12/2025",
            "10:00 AM",
            "Dr. Sharma",
            "Indore Clinic",
            "₹1200");
    Map<String, Object> example = new HashMap<>();
    example.put("body_text", List.of(exampleParams));
    body.put("example", example);

    components.add(body);

    return createTemplate(templateName, category, language, components);
  }

  /**
   * Get all message templates
   *
   * @return API response with list of templates
   */
  public String getAllTemplates() {
    String url = apiUrl + "/" + businessAccountId + "/message_templates";

    try {
      HttpEntity<Void> request = new HttpEntity<>(headers());
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.GET, request, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("✅ Templates retrieved successfully");
        return response.getBody();
      } else {
        log.error("❌ Failed to retrieve templates: {}", response.getBody());
        return response.getBody();
      }
    } catch (Exception e) {
      log.error("🚨 Template retrieval error: {}", e.getMessage(), e);
      return "Error: " + e.getMessage();
    }
  }

  /**
   * Delete a message template
   *
   * @param templateName Name of the template to delete
   * @return API response
   */
  public String deleteTemplate(String templateName) {
    String url = apiUrl + "/" + businessAccountId + "/message_templates?name=" + templateName;

    try {
      HttpEntity<Void> request = new HttpEntity<>(headers());
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("✅ Template deleted successfully: {}", templateName);
        return response.getBody();
      } else {
        log.error("❌ Failed to delete template: {}", response.getBody());
        return response.getBody();
      }
    } catch (Exception e) {
      log.error("🚨 Template deletion error: {}", e.getMessage(), e);
      return "Error: " + e.getMessage();
    }
  }
}
