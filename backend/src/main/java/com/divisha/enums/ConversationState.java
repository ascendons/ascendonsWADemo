package com.divisha.enums;

import java.util.EnumSet;
import java.util.Locale;

public enum ConversationState {
  START,
  APPOINTMENT_MENU_SENT,
  FOLLOWUP_SENT,
  ASK_LOCATION,
  ASK_NAME,
  ASK_AGE,
  ASK_GENDER,
  ASK_DOCTOR, // we ask for location → then doctor list
  ASK_DATE, // doctor selected → show date ranges
  ASK_EXPANDED_DATE, // after range → show per-day list
  ASK_HOUR, // show 2-hour blocks
  ASK_TIME, // show 15-min slots
  CONFIRM_BOOKING,
  CONFIRMED,
  CANCEL,
  CANCELLED,
  RESCHEDULE,
  WALK_IN,
  FLOW_SENT; // WhatsApp Flow sent to user

  private static final EnumSet<ConversationState> TERMINAL = EnumSet.of(CONFIRMED, CANCELLED);

  public boolean isTerminal() {
    return TERMINAL.contains(this);
  }

  public static ConversationState fromString(String v) {
    if (v == null) throw new IllegalArgumentException("state is null");
    return ConversationState.valueOf(
        v.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_'));
  }
}
