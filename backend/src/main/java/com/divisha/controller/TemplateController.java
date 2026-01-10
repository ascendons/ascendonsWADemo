package com.divisha.controller;

import com.divisha.service.WhatsAppTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing WhatsApp Message Templates
 *
 * <p>Use these endpoints to create, retrieve, and delete message templates via Meta Graph API.
 */
@Slf4j
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

  private final WhatsAppTemplateService templateService;

  /**
   * Create appointment cancellation template
   *
   * <p>POST /api/templates/appointment-cancellation
   *
   * @return API response
   */
  @PostMapping("/appointment-cancellation")
  public ResponseEntity<String> createCancellationTemplate() {
    log.info("📝 Creating appointment cancellation template...");
    String response = templateService.createAppointmentCancellationTemplate();
    return ResponseEntity.ok(response);
  }

  /**
   * Create appointment rescheduled template
   *
   * <p>POST /api/templates/appointment-rescheduled
   *
   * @return API response
   */
  @PostMapping("/appointment-rescheduled")
  public ResponseEntity<String> createRescheduledTemplate() {
    log.info("📝 Creating appointment rescheduled template...");
    String response = templateService.createAppointmentRescheduledTemplate();
    return ResponseEntity.ok(response);
  }

  /**
   * Create appointment booking confirmation template
   *
   * <p>POST /api/templates/appointment-confirmed
   *
   * @return API response
   */
  @PostMapping("/appointment-confirmed")
  public ResponseEntity<String> createConfirmedTemplate() {
    log.info("📝 Creating appointment confirmed template...");
    String response = templateService.createAppointmentConfirmedTemplate();
    return ResponseEntity.ok(response);
  }

  /**
   * Get all message templates
   *
   * <p>GET /api/templates
   *
   * @return List of all templates
   */
  @GetMapping
  public ResponseEntity<String> getAllTemplates() {
    log.info("📋 Fetching all templates...");
    String response = templateService.getAllTemplates();
    return ResponseEntity.ok(response);
  }

  /**
   * Delete a message template
   *
   * <p>DELETE /api/templates/{templateName}
   *
   * @param templateName Name of the template to delete
   * @return API response
   */
  @DeleteMapping("/{templateName}")
  public ResponseEntity<String> deleteTemplate(@PathVariable String templateName) {
    log.info("🗑️ Deleting template: {}", templateName);
    String response = templateService.deleteTemplate(templateName);
    return ResponseEntity.ok(response);
  }

  /**
   * Health check endpoint
   *
   * <p>GET /api/templates/health
   *
   * @return Health status
   */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Template API is healthy ✅");
  }
}
