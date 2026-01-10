package com.divisha.handler;

import com.divisha.enums.AppointmentStatus;
import com.divisha.enums.ConversationState;
import com.divisha.model.Appointment;
import com.divisha.model.Doctor;
import com.divisha.repository.DoctorRepository;
import com.divisha.service.*;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CancelHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;
  private final MetaMessageBuilder builder;
  private final AppointmentService appointmentService;
  private final DoctorRepository doctorRepository;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.CANCEL;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    // ✅ Step 1: If the user already selected a booking to cancel
    if (!listReply.isEmpty() && listReply.contains("BID-") && !isOwner) {

      Appointment appointment = appointmentService.findByBookingId(listReply);
      if (appointment == null) {
        sender.text(phone, "⚠️ Sorry, we couldn’t find that booking. Please try again.");
        context.clearState(phone);
        return;
      }

      // ✅ Step 2: Update appointment status to CANCELLED
      appointment.setStatus(AppointmentStatus.CANCELLED.toString());
      appointmentService.updateAppointment(appointment);

      Optional<Doctor> doctorIdObj = doctorRepository.findById(appointment.getDoctorId());
      String doctorName = "";
      if (doctorIdObj.isPresent()) {
        Doctor doctor = doctorIdObj.get();
        doctorName = doctor.getName();
      }
      doctorName = doctorName.isEmpty() ? appointment.getDoctorId() : doctorName;

      // ✅ Step 3: Notify user
      sender.text(
          phone,
          """
                    ❌ *Appointment Cancelled Successfully!*

                    🔖 *Booking ID:* %s
                    📅 *Date:* %s
                    🕒 *Time:* %s
                    🩺 *Doctor:* %s

                    We're sorry to see you cancel.
                    Reply anytime to *book again* — just say Hi! 👋
                    """
              .formatted(
                  appointment.getBookingId(),
                  appointment.getDate(),
                  appointment.getTime(),
                  doctorName));

      context.clearState(phone);
      return;
    }

    if (text.contains("Initiating")) {
      // ✅ Step 4: If user typed “Cancel”, show all active appointments
      List<Appointment> appointments =
          appointmentService.getUpcomingActiveAndWaitListedAppointments(phone).stream()
              .limit(10)
              .toList();

      if (appointments.isEmpty()) {
        sender.text(phone, "You don’t have any upcoming appointments to cancel.");
        context.clearState(phone);
        return;
      }

      // ✅ Step 5: Show list of appointments to cancel
      sender.sendList(phone, builder.showTheBookedAppointment(appointments));
    }
  }
}
