package com.divisha.handler;

import com.divisha.enums.ConversationState;
import java.util.Map;

public interface ConversationHandler {
  ConversationState state();

  void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner);
}
