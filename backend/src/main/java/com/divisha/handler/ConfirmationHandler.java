package com.divisha.handler;

import com.divisha.enums.AppointmentStatus;
import com.divisha.enums.ConversationState;
import com.divisha.model.Appointment;
import com.divisha.model.BookingConfirmationDTO;
import com.divisha.model.Doctor;
import com.divisha.repository.DoctorRepository;
import com.divisha.service.*;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfirmationHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;
  private final MetaMessageBuilder builder;
  private final AppointmentService appointmentService;
  private final DoctorRepository doctorRepository;

  @Value("${divisha.confirmation.fee:₹1200 (Payable at reception)}")
  private String feeText;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.CONFIRM_BOOKING;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    if (text == null) return;

    if ("CANCEL".equalsIgnoreCase(buttonReply)) {
      sender.text(
          phone,
          "❌ Your appointment request has been cancelled.\n\nReply anytime to book again — just say *Hi* and we're here to help!");
      context.clearState(phone);
      return;
    }

    if (!"CONFIRM".equalsIgnoreCase(buttonReply)) return;

    String date = context.getSelectedDate(phone);
    String time = context.getSelectedHourSlot(phone);
    String doctorId = context.getSelectedDoctor(phone);
    String patientId = context.getSelectedPatientId(phone);
    String locationId = context.getSelectedLocation(phone);
    String userName = context.getUserName(phone);
    String bookingId = "BID-" + System.currentTimeMillis() / 1000L;

    boolean slotAvailable = appointmentService.isSlotAvailable(date, time, doctorId);

    String status =
        slotAvailable
            ? AppointmentStatus.CONFIRMED.toString()
            : AppointmentStatus.WAITLISTED.toString();

    Appointment appointmentRequest =
        Appointment.builder()
            .doctorId(doctorId)
            .status(status)
            .patientId(patientId)
            .locationId(locationId)
            .bookingId(bookingId)
            .date(date)
            .time(time)
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

    BookingConfirmationDTO dto =
        new BookingConfirmationDTO(
            userName,
            context.getUserAge(phone),
            context.getUserGender(phone),
            doctorName,
            locationId,
            date,
            time,
            feeText,
            patientId,
            bookingId,
            phone,
            null);

    if (slotAvailable) {
      String rescheduledBookingId = context.getRescheduledBookingId(phone);
      if (!StringUtils.isEmpty(rescheduledBookingId)) {
        appointmentService.updateAppointmentStatus(
            rescheduledBookingId, AppointmentStatus.CANCELLED.toString(), false);
        sender.text(
            phone,
            builder.confirmationSuccessText(
                dto, "*Your Appointment is Rescheduled Successfully!*"));
        context.clearState(phone);
        return;
      }
      sender.text(phone, builder.confirmationSuccessText(dto, "*Your Appointment is Confirmed!*"));
    } else {
      sender.text(phone, builder.confirmationWaitListSuccessText(dto));
    }

    context.clearState(phone);
  }
}
