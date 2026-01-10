package com.divisha.service;

import com.divisha.enums.ConversationState;
import com.divisha.handler.ConversationHandler;
import com.divisha.handler.HandlerRegistry;
import com.divisha.util.Payloads;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;

  @Value("${meta.phone.number.id}")
  private String phoneNumberId;

  public void processIncomingMessage(Map<String, Object> payload) {
    String text = Payloads.text(payload);
    String phone = Payloads.phone(payload);
    String listReply = Payloads.listReply(payload);
    String buttonReply = "";

    ConversationState state = context.getState(phone);
    ConversationHandler handler = registry.get(state);

    log.info("➡️ phone={} state={} text='{}' list='{}'", phone, state, text, listReply);

    if (handler == null) {
      // No handler registered → reset to START and try again
      context.setState(phone, ConversationState.START);
      handler = registry.get(ConversationState.START);
    }

    handler.handle(phone, text, listReply, buttonReply, payload, true);
  }

  public void processIncomingMetaMessage(Map<String, Object> payload) {

    try {
      List<?> entry = (List<?>) payload.get("entry");
      Map<?, ?> entryObj = (Map<?, ?>) entry.get(0);

      List<?> changes = (List<?>) entryObj.get("changes");
      Map<?, ?> change = (Map<?, ?>) changes.get(0);

      Map<?, ?> value = (Map<?, ?>) change.get("value");

      List<?> messages = (List<?>) value.get("messages");

      if (messages == null || messages.isEmpty()) {
        log.info("📭 No user messages in payload");
        return;
      }

      Map<?, ?> msg = (Map<?, ?>) messages.get(0);

      String from = msg.get("from").toString();
      String type = msg.get("type").toString();

      log.info("📥 Meta message from {} type={}", from, type);
      String body = "";
      String listReply = "";
      String buttonReply = "";
      switch (type) {
        case "text" -> {
          Map<?, ?> text = (Map<?, ?>) msg.get("text");
          body = text.get("body").toString();
          log.info("💬 User said: {}", body);
          // process user text message
        }

        case "interactive" -> {
          Map<?, ?> interactive = (Map<?, ?>) msg.get("interactive");
          // interactive button or list response
          Map<?, ?> obj = (Map<?, ?>) interactive.get("button_reply");
          Map<?, ?> list = (Map<?, ?>) interactive.get("list_reply");

          buttonReply = obj == null ? "" : obj.get("title").toString();
          listReply = list == null ? "" : list.get("id").toString();

          log.info("🎛 Interactive response: {}", interactive);
        }

        default -> log.warn("⚠ Unsupported message type: {}", type);
      }
      ConversationState state = context.getState(from);
      ConversationHandler handler = registry.get(state);

      log.info("➡️ phone={} state={} text='{}' list='{}'", from, state, body, listReply);

      if (handler == null) {
        // No handler registered → reset to START and try again
        context.setState(from, ConversationState.START);
        handler = registry.get(ConversationState.START);
      }
      if (body.equalsIgnoreCase("exit")) {
        context.clearState(from);
        return;
      }

      boolean isOwner = Objects.equals(phoneNumberId, from);
      handler.handle(from, body, listReply, buttonReply, payload, isOwner);

    } catch (Exception e) {
      log.error("❌ Failed to parse Meta webhook: {}", e.getMessage(), e);
    }
  }
}
