package com.divisha.handler;

import com.divisha.enums.AppointmentStatus;
import com.divisha.enums.ConversationState;
import com.divisha.flow.dto.DropdownOption;
import com.divisha.flow.service.AppointmentFlowService;
import com.divisha.model.Appointment;
import com.divisha.model.MetaFlowMessage;
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
public class RescheduleHandler implements ConversationHandler {
  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;
  private final MetaMessageBuilder builder;
  private final AppointmentService appointmentService;
  private final AppointmentFlowService appointmentFlowService;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Value("${meta.flow.appointment.booking.id}")
  private String appointmentFlowId;

  @Override
  public ConversationState state() {
    return ConversationState.RESCHEDULE;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    if (!listReply.isEmpty() && listReply.contains("BID-") && !isOwner) {
      Appointment appointment = appointmentService.findByBookingId(listReply);
      if (appointment == null) {
        sender.text(phone, "⚠️ Sorry, we could’t find that booking. Please try again.");
        context.clearState(phone);
        return;
      }
      context.setReScheduleBookingId(phone, listReply);
      context.setState(phone, ConversationState.FOLLOWUP_SENT);
      String rescheduledBookingId = context.getRescheduledBookingId(phone);
      if (rescheduledBookingId != null && !rescheduledBookingId.isEmpty()) {
        appointmentService.updateAppointmentStatus(
            rescheduledBookingId, AppointmentStatus.CANCELLED.name(), false);
      }
      sendAppointmentFlow(phone, appointment.getPatientId());
    }
    if (text.contains("Initiating")) {
      List<Appointment> appointments =
          appointmentService.getUpcomingActiveAndWaitListedAppointments(phone).stream()
              .limit(10)
              .toList();

      if (appointments.isEmpty()) {
        sender.text(phone, "You don’t have any upcoming appointments to Reschedule.");
        context.clearState(phone);
      } else {
        sender.sendList(phone, builder.showTheBookedAppointmentToBeRescheduled(appointments));
      }
    }
  }

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
