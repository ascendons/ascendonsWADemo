package com.divisha.handler;

import com.divisha.enums.AppointmentStatus;
import com.divisha.enums.ConversationState;
import com.divisha.model.Appointment;
import com.divisha.model.Doctor;
import com.divisha.repository.DoctorRepository;
import com.divisha.service.AppointmentService;
import com.divisha.service.ConversationContextStore;
import com.divisha.service.MetaMessageBuilder;
import com.divisha.service.MetaMessageSender;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalkInHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;
  private final MetaMessageBuilder builder;
  private final AppointmentService appointmentService;
  private final DoctorRepository doctorRepository;

  @Value("${divisha.confirmation.fee:₹500 (Payable at reception)}")
  private String feeText;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.WALK_IN;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    if (buttonReply.isEmpty()) return;
    String bookingId = "BID-" + System.currentTimeMillis() / 1000L;
    context.setSelectedDate(phone, buttonReply);
    String doctorId = context.getSelectedDoctor(phone);
    Appointment appointmentRequest =
        Appointment.builder()
            .doctorId(doctorId)
            .status(AppointmentStatus.WALK_IN.name())
            .patientId(context.getSelectedPatientId(phone))
            .locationId(context.getSelectedLocation(phone))
            .bookingId(bookingId)
            .date(context.getSelectedDate(phone).replace("-", ""))
            .time(null)
            .phone(phone)
            .createdTs(System.currentTimeMillis() / 1000L)
            .build();

    appointmentService.createAppointment(appointmentRequest);
    Optional<Doctor> doctorIdObj = doctorRepository.findById(doctorId);
    String doctorName = "";
    if (doctorIdObj.isPresent()) {
      Doctor doctor = doctorIdObj.get();
      doctorName = doctor.getName();
    }
    doctorName = doctorName.isEmpty() ? doctorId : doctorName;
    sender.text(
        phone,
        """
                ✅ *Your Walk-In Appointment Confirmed Successfully!*

                👤 *Name:* %s
                🩺 *Doctor:* %s
                📍 *Location:* %s
                📅 *Date:* %s
                💰 *Consultation Fee:* %s
                🆔 *Patient ID:* %s
                🔖 *Booking ID:* %s

                Thank you for choosing Divisha Arthritis & Medical Center 🙏
                """
            .formatted(
                context.getUserName(phone),
                doctorName,
                context.getSelectedLocation(phone),
                context.getSelectedDate(phone),
                feeText,
                context.getSelectedPatientId(phone),
                bookingId));

    context.clearState(phone);
  }
}
