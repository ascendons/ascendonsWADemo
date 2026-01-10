package com.divisha.handler;

import com.divisha.enums.ConversationState;
import com.divisha.service.ConversationContextStore;
import com.divisha.service.MetaMessageSender;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewPatientAgeHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.ASK_AGE;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    try {
      if (isOwner) {
        return;
      }
      int age = Integer.parseInt(text.trim());
      context.setUserAge(phone, age);

      sender.text(phone, "Got it ✅\nNow please share your Gender (Male/Female/Other):");
      context.setState(phone, ConversationState.ASK_GENDER);
      return;
    } catch (Exception e) {
      sender.text(phone, "Please enter a valid number for Age:");
      return;
    }
  }
}
