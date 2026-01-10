package com.divisha.handler;

import com.divisha.enums.ConversationState;
import com.divisha.model.BookingConfirmationDTO;
import com.divisha.model.Doctor;
import com.divisha.repository.DoctorRepository;
import com.divisha.service.*;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SelectTimeHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;
  private final MetaMessageBuilder builder;
  private final DoctorRepository doctorRepository;

  @Value("${divisha.confirmation.fee:₹1200 (Payable at reception)}")
  private String feeText;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.ASK_TIME;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    if (listReply != null && listReply.startsWith("SLOT_")) {
      String time = listReply.substring(listReply.lastIndexOf("_") + 1);
      String date = context.getSelectedDate(phone);
      context.setSelectedHourSlot(phone, time);

      String doctorId = context.getSelectedDoctor(phone);
      Optional<Doctor> doctorIdObj = doctorRepository.findById(doctorId);
      String doctorName = "";
      if (doctorIdObj.isPresent()) {
        Doctor doctor = doctorIdObj.get();
        doctorName = doctor.getName();
      }
      doctorName = doctorName.isEmpty() ? doctorId : doctorName;

      // Static demo data — swap with your data source
      BookingConfirmationDTO dto =
          new BookingConfirmationDTO(
              context.getUserName(phone),
              context.getUserAge(phone),
              context.getUserGender(phone),
              doctorName,
              context.getSelectedLocation(phone),
              date,
              time,
              feeText,
              context.getSelectedPatientId(phone),
              null,
              phone,
              null);

      sender.sendButtons(phone, builder.confirmationButtons(dto));
      context.setState(phone, ConversationState.CONFIRM_BOOKING);
    }
  }
}
