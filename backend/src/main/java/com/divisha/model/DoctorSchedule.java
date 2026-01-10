package com.divisha.model;

import java.util.List;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "doctor_schedule")
public class DoctorSchedule {

  @Id private String id;

  private String doctorId;
  private String doctorName;
  private String locationId;

  // Standard recurring work hours
  private String startTime; // "09:00"
  private String endTime; // "17:00"

  /** Doctor is NOT available on these days ALWAYS Example: ["SUNDAY"] */
  private List<String> unavailableDaysOfWeek;

  /** Specific calendar dates doctor is NOT available Example: ["2025-11-27","2025-12-25"] */
  private List<String> unavailableDates;

  /**
   * Custom slots for certain dates Example: doctor works 10 AM – 2 PM only on Xmas [ { "date":
   * "2025-12-25", "startTime": "10:00", "endTime": "14:00", "slotDurationMinutes": 30 } ]
   */
  private List<CustomDateSlot> customDateSlots;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CustomDateSlot {
    private String date; // yyyy-MM-dd
    private String startTime; // HH:mm
    private String endTime; // HH:mm
    private Integer slotDurationMinutes; // ex: 15, 30, 10
  }
}
