package com.divisha.handler;

import com.divisha.enums.ConversationState;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HandlerRegistry {

  private final Map<ConversationState, ConversationHandler> map =
      new EnumMap<>(ConversationState.class);

  public HandlerRegistry register(ConversationHandler handler) {
    map.put(handler.state(), handler);
    return this;
  }

  public ConversationHandler get(ConversationState state) {
    return map.get(state);
  }
}
