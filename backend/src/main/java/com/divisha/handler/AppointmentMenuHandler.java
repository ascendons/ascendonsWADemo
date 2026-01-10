package com.divisha.handler;

import com.divisha.enums.ConversationState;
import com.divisha.service.*;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentMenuHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  //  private final WatiMessageSender sender;
  private final MetaMessageSender metaMessageSender;
  private final MetaMessageBuilder metaMessageBuilder;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.APPOINTMENT_MENU_SENT;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    if (equalsIgnoreCase(buttonReply, "BOOK APPOINTMENT")) {
      //            sender.buttons(phone, builder.followupMenu());
      //      metaMessageSender.text(phone, "Fetching your existing details");
      context.setState(phone, ConversationState.FOLLOWUP_SENT);
      ConversationState state = context.getState(phone);
      ConversationHandler handler = registry.get(state);
      handler.handle(phone, "Fetching your existing details", "", "", new HashMap<>(), true);
    } else if (equalsIgnoreCase(buttonReply, "CANCEL/RESCHEDULE")) {
      metaMessageSender.sendButtons(phone, metaMessageBuilder.appointmentCancelRescheduleMenu());
    } else if (equalsIgnoreCase(buttonReply, "CANCEL")) {
      //      metaMessageSender.text(phone, "Initiating the appointment cancellation");
      context.setUserEnabledCancellation(phone, true);
      context.setState(phone, ConversationState.CANCEL);

      ConversationState state = context.getState(phone);
      ConversationHandler handler = registry.get(state);
      handler.handle(
          phone, "Initiating the appointment cancellation", "", "", new HashMap<>(), true);

    } else if (equalsIgnoreCase(buttonReply, "RESCHEDULE")) {
      //      metaMessageSender.text(phone, "Initiating the appointment reschedule");
      context.setState(phone, ConversationState.RESCHEDULE);

      ConversationState state = context.getState(phone);
      ConversationHandler handler = registry.get(state);
      handler.handle(phone, "Initiating the appointment reschedule", "", "", new HashMap<>(), true);
    } else if (equalsIgnoreCase(buttonReply, "WALK-IN/EMERGENCY")) {
      //      metaMessageSender.text(phone, "Fetching your existing details");
      context.setState(phone, ConversationState.FOLLOWUP_SENT);
      ConversationState state = context.getState(phone);
      ConversationHandler handler = registry.get(state);
      context.setUserCurrentFlow(phone, "WALK-IN/EMERGENCY");
      handler.handle(phone, "Fetching your existing details", "", "", new HashMap<>(), true);
    }
  }

  private boolean equalsIgnoreCase(String a, String b) {
    return a != null && a.equalsIgnoreCase(b);
  }
}
