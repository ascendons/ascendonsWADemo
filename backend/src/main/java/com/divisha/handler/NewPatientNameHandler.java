package com.divisha.handler;

import com.divisha.enums.ConversationState;
import com.divisha.enums.Gender;
import com.divisha.flow.dto.DropdownOption;
import com.divisha.flow.service.AppointmentFlowService;
import com.divisha.model.Location;
import com.divisha.model.MetaFlowMessage;
import com.divisha.model.Patient;
import com.divisha.model.RegisterPatient;
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
public class NewPatientNameHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;
  private final LocationService locationService;
  private final PatientService patientService;
  private final MetaMessageBuilder builder;
  private final AppointmentFlowService appointmentFlowService;

  @Value("${meta.flow.appointment.booking.id}")
  private String appointmentFlowId;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.ASK_NAME;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    if (isOwner) {
      return;
    }
    if (text == null || text.isBlank()) {
      sender.text(phone, "Name cannot be empty. Please provide your Full Name:");
      return;
    }

    context.setUserName(phone, text.trim());
    // commenting for divisha
    //    sender.text(phone, "Thanks! Now please enter your Age:");
    //    context.setState(phone, ConversationState.ASK_GENDER);
    RegisterPatient req = buildRegisterRequest(phone);
    try {
      Patient registered = patientService.createPatient(req);
      context.setSelectedPatientId(phone, registered.getPatientId());

      sender.text(
          phone,
          "✅ *Registration Successful!*\nYour Patient ID: *" + registered.getPatientId() + "*");

      proceedToLocationSelection(phone);

    } catch (Exception e) {
      sender.text(
          phone,
          "❗ *Registration failed:* "
              + e.getMessage()
              + "\n\n👉 Please enter your *full name* again.");
      context.setState(phone, ConversationState.ASK_NAME);
    }
  }

  private RegisterPatient buildRegisterRequest(String phone) {
    return RegisterPatient.builder()
        .phone(phone)
        .name(context.getUserName(phone))
        .age(context.getUserAge(phone))
        .gender(Gender.UNDISCLOSED)
        .build();
  }

  private void proceedToLocationSelection(String phone) {
    String patientId = context.getSelectedPatientId(phone);

    // Check if this is a walk-in/emergency flow
    String userFlow = context.getUserCurrentFlow(phone);
    if (userFlow != null && userFlow.contains("WALK-IN/EMERGENCY")) {
      // Keep existing walk-in flow (message-based)
      List<Location> locations = locationService.getAllActiveLocation();

      if (locations.isEmpty()) {
        sender.text(
            phone,
            "🙏 Thank you!\nWe are currently not available in your area. "
                + "We will notify you once we launch.");
        context.clearState(phone);
        return;
      }
      context.setState(phone, ConversationState.ASK_DOCTOR);
      if (locations.size() == 1) {
        ConversationState state = context.getState(phone);
        ConversationHandler handler = registry.get(state);
        handler.handle(phone, "", "", locations.get(0).getName(), null, true);
      } else {
        sender.sendButtons(phone, builder.locationMenu(locations));
      }
    } else {
      sendAppointmentFlow(phone, patientId);
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
