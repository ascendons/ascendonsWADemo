package com.divisha.handler;

import com.divisha.enums.ConversationState;
import com.divisha.service.*;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SelectHourHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;
  private final MetaMessageBuilder builder;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.ASK_HOUR;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    if (listReply != null && listReply.startsWith("TIME_")) {
      String date = context.getSelectedDate(phone);
      String hourRange = listReply.substring(listReply.lastIndexOf("_") + 1);
      context.setSelectedHourSlot(phone, hourRange);

      sender.sendList(phone, builder.fifteenMinuteSlots(date, hourRange));
      context.setState(phone, ConversationState.ASK_TIME);
    }
  }
}
