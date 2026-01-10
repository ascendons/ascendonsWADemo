package com.divisha.handler;

import com.divisha.enums.ConversationState;
import com.divisha.service.*;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender metaMessageSender;
  private final MetaMessageBuilder metaMessageBuilder;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.START;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    if (text != null && (text.equalsIgnoreCase("hi") || text.equalsIgnoreCase("hello"))) {
      metaMessageSender.sendButtons(phone, metaMessageBuilder.appointmentMenu());
      context.setState(phone, ConversationState.APPOINTMENT_MENU_SENT);
    }
  }
}
