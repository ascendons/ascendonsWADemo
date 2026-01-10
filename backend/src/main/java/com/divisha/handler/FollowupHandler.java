package com.divisha.handler;

import com.divisha.enums.AppointmentStatus;
import com.divisha.enums.ConversationState;
import com.divisha.flow.dto.DropdownOption;
import com.divisha.flow.service.AppointmentFlowService;
import com.divisha.model.Location;
import com.divisha.model.MetaFlowMessage;
import com.divisha.model.Patient;
import com.divisha.service.*;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowupHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;
  private final MetaMessageBuilder builder;
  private final PatientService patientService;
  private final LocationService locationService;
  private final AppointmentService appointmentService;
  private final AppointmentFlowService appointmentFlowService;

  @Value("${meta.flow.appointment.booking.id}")
  private String appointmentFlowId;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.FOLLOWUP_SENT;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    List<Patient> patientList = patientService.getPatientByPhone(phone);
    if (patientList.isEmpty()) {
      sender.text(phone, "Please share your full name:");
      context.setState(phone, ConversationState.ASK_NAME);
    } else if (!isOwner && buttonReply.contains("Add New Patient")) {
      sender.text(phone, "Please share your full name:");
      context.setState(phone, ConversationState.ASK_NAME);
    } else if (!isOwner) {
      for (Patient patient : patientList) {
        if (patient.getName().equalsIgnoreCase(buttonReply.trim())) {
          context.setUserName(phone, patient.getName());
          context.setSelectedPatientId(phone, patient.getPatientId());

          // Check if this is a walk-in/emergency flow
          String userFlow = context.getUserCurrentFlow(phone);
          if (userFlow != null && userFlow.contains("WALK-IN/EMERGENCY")) {
            // Keep existing walk-in flow (message-based)
            List<Location> allActiveLocation = locationService.getAllActiveLocation();
            if (allActiveLocation.isEmpty()) {
              sender.text(phone, "No data available");
              context.clearState(phone);
              context.setState(phone, ConversationState.CANCELLED);
            } else if (allActiveLocation.size() == 1) {
              context.setState(phone, ConversationState.ASK_DOCTOR);
              ConversationState state = context.getState(phone);
              ConversationHandler handler = registry.get(state);
              handler.handle(phone, "", "", allActiveLocation.get(0).getName(), null, true);
            } else {
              context.setState(phone, ConversationState.ASK_DOCTOR);
              sender.sendButtons(phone, builder.locationMenu(allActiveLocation));
            }
          } else {
            // Regular appointment - use WhatsApp Flow
            String rescheduledBookingId = context.getRescheduledBookingId(phone);
            if (rescheduledBookingId != null && !rescheduledBookingId.isEmpty()) {
              appointmentService.updateAppointmentStatus(
                  rescheduledBookingId, AppointmentStatus.CANCELLED.name(), false);
            }
            sendAppointmentFlow(phone, patient.getPatientId());
          }

          return;
        }
      }
    } else if (text.equalsIgnoreCase("Fetching your existing details")) {
      sender.sendButtons(phone, builder.followUpMenu(patientList));
    }
  }

  /**
   * Send WhatsApp Flow for appointment booking
   *
   * @param phone Phone number
   * @param patientId Patient ID
   */
  private void sendAppointmentFlow(String phone, String patientId) {
    try {
      // Fetch dynamic doctor data from database
      List<DropdownOption> doctors = appointmentFlowService.getDoctorOptions();

      if (doctors.isEmpty()) {
        sender.text(phone, "⚠️ No doctors available at the moment. Please try again later.");
        context.clearState(phone);
        return;
      }

      // Prepare initial data for the flow
      Map<String, Object> initialData = new HashMap<>();
      initialData.put("doctors", doctors);
      initialData.put("locations", new ArrayList<>());
      initialData.put("dates", new ArrayList<>());
      initialData.put("time_slots", new ArrayList<>());
      initialData.put("is_location_enabled", false);
      initialData.put("is_date_enabled", false);
      initialData.put("is_time_enabled", false);
      initialData.put("patient_id", patientId);
      initialData.put("phone_number", phone);

      // Build flow message
      MetaFlowMessage flowMessage =
          MetaFlowMessage.builder()
              .header("Book Your Appointment")
              .body("Select your preferred doctor, location, date and time for your appointment.")
              .footer("Your health is our priority")
              .flowId(appointmentFlowId)
              .flowCta("Book Now")
              .flowAction("navigate")
              .screenId("APPOINTMENT")
              .flowActionPayload(initialData)
              .build();

      // Send flow message
      sender.sendFlow(phone, flowMessage);

      // Update conversation state to indicate flow sent
      context.setState(phone, ConversationState.FLOW_SENT);

    } catch (Exception e) {
      sender.text(
          phone,
          "❌ Sorry, we encountered an error while loading the appointment form. Please try again by typing 'Hi'.");
      context.clearState(phone);
    }
  }
}
