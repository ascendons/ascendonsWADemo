package com.divisha.service;

import com.divisha.enums.ConversationState;
import com.divisha.model.FlowResponseData;
import com.divisha.model.MetaTextMessage;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowService {

  private final ConversationContextStore contextStore;
  private final MetaMessageSender messageSender;
  private final PatientService patientService;
  private final MessageService messageService;
  private final com.divisha.flow.service.AppointmentFlowService appointmentFlowService;
  private final com.divisha.repository.DoctorRepository doctorRepository;
  private final com.divisha.repository.LocationRepository locationRepository;

  /** Process flow completion based on flow type */
  public void processFlowCompletion(String phoneNumber, FlowResponseData flowResponse) {
    log.info(
        "🔄 Processing flow completion for {}: flowId={}, action={}",
        phoneNumber,
        flowResponse.getFlowId(),
        flowResponse.getAction());

    try {
      Map<String, Object> data = flowResponse.getData();
      String flowId = flowResponse.getFlowId();

      // Route to appropriate handler based on flow ID
      if (flowId.contains("patient_registration") || flowId.contains("PATIENT_REG")) {
        handlePatientRegistrationFlow(phoneNumber, data);
      } else if (flowId.contains("appointment_booking") || flowId.contains("APPOINTMENT")) {
        handleAppointmentBookingFlow(phoneNumber, data);
      } else if (flowId.contains("reschedule") || flowId.contains("RESCHEDULE")) {
        handleRescheduleFlow(phoneNumber, data);
      } else if (flowId.contains("cancel") || flowId.contains("CANCEL")) {
        handleCancelFlow(phoneNumber, data);
      } else {
        log.warn("⚠️ Unknown flow type: {}", flowId);
        sendErrorMessage(phoneNumber, "Sorry, I couldn't process that request.");
      }

    } catch (Exception e) {
      log.error("❌ Error processing flow completion for {}: {}", phoneNumber, e.getMessage(), e);
      sendErrorMessage(
          phoneNumber, "An error occurred while processing your request. Please try again.");
    }
  }

  /** Handle patient registration flow completion */
  private void handlePatientRegistrationFlow(String phoneNumber, Map<String, Object> data) {
    log.info("👤 Processing patient registration flow for {}", phoneNumber);

    try {
      // Extract patient details from flow data
      String name = (String) data.get("name");
      String ageStr = (String) data.get("age");
      String gender = (String) data.get("gender");

      if (name == null || ageStr == null || gender == null) {
        log.warn("⚠️ Missing required fields in patient registration flow");
        sendErrorMessage(phoneNumber, "Please provide all required information.");
        return;
      }

      int age = Integer.parseInt(ageStr);

      // Store patient data in context
      contextStore.setName(phoneNumber, name);
      contextStore.setAge(phoneNumber, age);
      contextStore.setGender(phoneNumber, gender);

      log.info("✅ Patient details stored: name={}, age={}, gender={}", name, age, gender);

      // Update conversation state to next step (location selection)
      contextStore.setState(phoneNumber, ConversationState.ASK_LOCATION);

      // Trigger location selection handler
      messageService.processIncomingMetaMessage(
          buildMetaWebhookPayload(phoneNumber, "continue", "text"));

    } catch (Exception e) {
      log.error("❌ Error handling patient registration flow: {}", e.getMessage(), e);
      sendErrorMessage(phoneNumber, "Failed to register. Please try again.");
    }
  }

  /** Handle appointment booking flow completion */
  private void handleAppointmentBookingFlow(String phoneNumber, Map<String, Object> data) {
    log.info("📅 Processing appointment booking flow for {}", phoneNumber);

    try {
      // Extract appointment details
      String locationId = (String) data.get("location");
      String doctorId = (String) data.get("doctor");
      String date = (String) data.get("date");
      String timeSlot = (String) data.get("time_slot");
      String patientId = (String) data.get("patient_id");

      if (locationId == null || doctorId == null || date == null || timeSlot == null) {
        log.warn("⚠️ Missing required fields in appointment booking flow");
        sendErrorMessage(phoneNumber, "Please provide all required information.");
        contextStore.clearState(phoneNumber);
        return;
      }

      if (patientId == null) {
        log.warn("⚠️ Missing patient_id in appointment booking flow");
        sendErrorMessage(phoneNumber, "Patient information missing. Please try booking again.");
        contextStore.clearState(phoneNumber);
        return;
      }

      // Create appointment using the flow service (appointment is created in the flow itself via
      // data_exchange)
      // This code handles the webhook response after flow completion
      // The appointment should already be created, so we just need to confirm

      // Get doctor and location names for confirmation message
      String doctorName =
          doctorRepository
              .findById(doctorId)
              .map(com.divisha.model.Doctor::getName)
              .orElse("Doctor");
      String locationName =
          locationRepository
              .findById(locationId)
              .map(com.divisha.model.Location::getName)
              .orElse("Location");

      // Format date for display
      String displayDate = formatDateForDisplay(date);

      // Note: The appointment creation happens in the flow data_exchange endpoint
      // This is just to handle any additional processing after flow completion
      log.info(
          "✅ Flow completed - Doctor: {}, Location: {}, Date: {}, Time: {}, Patient: {}",
          doctorName,
          locationName,
          displayDate,
          timeSlot,
          patientId);

      // Clear conversation state
      contextStore.clearState(phoneNumber);

    } catch (Exception e) {
      log.error("❌ Error handling appointment booking flow: {}", e.getMessage(), e);
      sendErrorMessage(phoneNumber, "Failed to process appointment. Please contact support.");
      contextStore.clearState(phoneNumber);
    }
  }

  /** Format date from yyyy-MM-dd to readable format */
  private String formatDateForDisplay(String dateStr) {
    try {
      java.time.LocalDate date =
          java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
      return date.format(
          java.time.format.DateTimeFormatter.ofPattern(
              "EEE, MMM dd, yyyy", java.util.Locale.ENGLISH));
    } catch (Exception e) {
      return dateStr;
    }
  }

  /** Handle reschedule flow completion */
  private void handleRescheduleFlow(String phoneNumber, Map<String, Object> data) {
    log.info("🔄 Processing reschedule flow for {}", phoneNumber);
    // TODO: Implement reschedule flow logic
    sendSuccessMessage(phoneNumber, "Reschedule request received. Processing...");
  }

  /** Handle cancel flow completion */
  private void handleCancelFlow(String phoneNumber, Map<String, Object> data) {
    log.info("❌ Processing cancel flow for {}", phoneNumber);
    // TODO: Implement cancel flow logic
    sendSuccessMessage(phoneNumber, "Cancel request received. Processing...");
  }

  /** Send error message to user */
  private void sendErrorMessage(String phoneNumber, String message) {
    try {
      MetaTextMessage textMessage = MetaTextMessage.builder().text(message).build();
      messageSender.sendText(phoneNumber, textMessage);
    } catch (Exception e) {
      log.error("Failed to send error message: {}", e.getMessage());
    }
  }

  /** Send success message to user */
  private void sendSuccessMessage(String phoneNumber, String message) {
    try {
      MetaTextMessage textMessage = MetaTextMessage.builder().text(message).build();
      messageSender.sendText(phoneNumber, textMessage);
    } catch (Exception e) {
      log.error("Failed to send success message: {}", e.getMessage());
    }
  }

  /** Build a Meta webhook payload structure for triggering handlers */
  private Map<String, Object> buildMetaWebhookPayload(String from, String text, String type) {
    return Map.of(
        "entry",
        java.util.List.of(
            Map.of(
                "changes",
                java.util.List.of(
                    Map.of(
                        "value",
                        Map.of(
                            "messages",
                            java.util.List.of(
                                Map.of(
                                    "from", from,
                                    "type", type,
                                    "text", Map.of("body", text)))))))));
  }
}
