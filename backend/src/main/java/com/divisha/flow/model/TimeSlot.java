package com.divisha.flow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** TimeSlot entity representing an available appointment time. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {
  private String id;
  private String time;
  private boolean available;
  private String displayTime;

  public String getDisplayName() {
    return displayTime != null ? displayTime : time;
  }
}
