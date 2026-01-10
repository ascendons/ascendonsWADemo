package com.divisha.handler;

import com.divisha.enums.ConversationState;
import com.divisha.model.DoctorSchedule;
import com.divisha.repository.DoctorScheduleRepository;
import com.divisha.service.*;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SelectDateHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;
  private final MetaMessageBuilder builder;
  private final DoctorScheduleRepository doctorScheduleRepository;
  private final SlotService slotService;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.ASK_DATE;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    // Step 1: doctor selected (button text starts with "Dr")
    if (listReply != null && listReply.toUpperCase().startsWith("DOC_")) {
      String docId = listReply.replace("DOC_", "");
      context.setSelectedDoctor(phone, docId);
      List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorId(docId);
      DoctorSchedule doctorSchedule = schedules.isEmpty() ? null : schedules.get(0);
      context.setDoctorSchedule(phone, doctorSchedule);
      List<String> nextAvailableDates = slotService.getNextAvailableDates(doctorSchedule);

      if (context.getUserCurrentFlow(phone) != null
          && context.getUserCurrentFlow(phone).contains("WALK-IN/EMERGENCY")) {
        Collections.sort(nextAvailableDates);
        List<String> top3availableDates = nextAvailableDates.subList(0, 3);
        sender.sendButtons(phone, builder.top3AvailableDates(top3availableDates));
        context.setState(phone, ConversationState.WALK_IN);
        return;
      }
      if (nextAvailableDates.size() < 10) {
        sender.sendList(phone, builder.expandedDateListWhenLesserThen10(nextAvailableDates));
        context.setState(phone, ConversationState.ASK_EXPANDED_DATE);
      } else {
        sender.sendList(phone, builder.nextMonthRanges(nextAvailableDates));
      }
      return;
    }

    // Step 2: date-range selected
    if (listReply != null && listReply.startsWith("DATE_")) {
      String[] parts = listReply.replace("DATE_", "").split("_TO_");
      String start = parts[0];
      String end = parts[1];
      context.setSelectedDateRange(phone, start + "," + end);
      List<DoctorSchedule> schedules =
          doctorScheduleRepository.findByDoctorId(context.getSelectedDoctor(phone));
      DoctorSchedule doctorSchedule = schedules.isEmpty() ? null : schedules.get(0);
      context.setDoctorSchedule(phone, doctorSchedule);
      List<String> nextAvailableDates = slotService.getNextAvailableDates(doctorSchedule);

      sender.sendList(phone, builder.expandedDateList(start, end, nextAvailableDates));
      context.setState(phone, ConversationState.ASK_EXPANDED_DATE);
    }
  }
}
