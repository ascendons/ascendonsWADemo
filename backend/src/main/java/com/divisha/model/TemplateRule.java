package com.divisha.model;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRule {

  // stable unique id per rule for tracking
  private String ruleId = UUID.randomUUID().toString();

  @NotNull private DayOfWeek dayOfWeek;

  @NotNull private LocalTime start;

  @NotNull private LocalTime end;

  @NotNull private String locationId;

  private Recurrence recurrence = Recurrence.WEEKLY;

  private MonthOccurrence monthOccurrence;

  public enum Recurrence {
    WEEKLY,
    BIWEEKLY,
    MONTHLY
  }

  public enum MonthOccurrence {
    FIRST,
    SECOND,
    THIRD,
    FOURTH,
    LAST
  }
}

/**
 * Dr Vijay KR Rao's Consulting Timings
 *
 * <p>Location Days/Availability Timings 1. Divisha Arthritis and Medical Centre, Basaveshwara Nagar
 * Monday to Saturday 9:00 AM to 6:00 PM 2. Agilus Diagnostics Ltd, Brookfield (Whitefield Branch)
 * Alternate Sundays 9:00 AM to 2:00 PM 3. Agilus Diagnostics Ltd, Basavanagudi Branch One Wednesday
 * per month 3:00 PM to 7:00 PM
 */
