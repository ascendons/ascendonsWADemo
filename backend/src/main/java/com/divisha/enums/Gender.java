package com.divisha.enums;

import java.util.Arrays;

public enum Gender {
  MALE("Male"),
  FEMALE("Female"),
  OTHER("Others"),
  UNDISCLOSED("Undisclosed");

  private final String displayName;

  Gender(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  /**
   * Converts input string to Gender enum. Supports: - display names ("Male", "Female", etc.) - enum
   * names ("MALE", "FEMALE", etc.) - case-insensitive matching
   */
  public static Gender from(String value) {
    if (value == null) return Gender.OTHER;

    return Arrays.stream(Gender.values())
        .filter(g -> g.displayName.equalsIgnoreCase(value) || g.name().equalsIgnoreCase(value))
        .findFirst()
        .orElse(Gender.OTHER);
  }
}
