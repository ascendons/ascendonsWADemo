package com.divisha.util;

import java.util.Map;

@SuppressWarnings("unchecked")
public final class Payloads {

  private Payloads() {}

  public static String text(Map<String, Object> payload) {
    Object t = payload.get("text");
    return t instanceof String ? (String) t : "";
  }

  public static String phone(Map<String, Object> payload) {
    Object w = payload.get("waId");
    return w instanceof String ? (String) w : "";
  }

  public static String listReply(Map<String, Object> payload) {
    Object lr = payload.get("listReply");
    if (lr instanceof Map<?, ?> m) {
      Object desc = m.get("description");
      if (desc instanceof String s) return s;
      Object miss = m.get("desription"); // fallback if provider typo
      if (miss instanceof String s2) return s2;
    }
    return "";
  }
}
