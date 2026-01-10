package com.divisha.handler;

import com.divisha.enums.ConversationState;
import com.divisha.model.DoctorSchedule;
import com.divisha.service.*;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SelectExpandedDateHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;
  private final MetaMessageBuilder builder;
  private final SlotService slotService;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.ASK_EXPANDED_DATE;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    if (listReply != null && listReply.startsWith("DATE_")) {
      String selectedDate = listReply.replace("DATE_", "");
      context.setSelectedDate(phone, selectedDate);
      DoctorSchedule doctorSchedule = context.getDoctorSchedule(phone);
      List<String> availableHoursForDate =
          slotService.getAvailableHoursForDate(doctorSchedule, selectedDate);
      sender.sendList(phone, builder.twoHourSlots(selectedDate, availableHoursForDate));
      context.setState(phone, ConversationState.ASK_HOUR);
    }
  }
}
