package com.divisha.flow.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Appointment entity representing a booked appointment. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
  private String appointmentId;
  private String doctorId;
  private String doctorName;
  private String locationId;
  private String locationName;
  private String date;
  private String timeSlot;
  private String patientPhone;
  private LocalDateTime createdAt;
  private AppointmentStatus status;

  public enum AppointmentStatus {
    CONFIRMED,
    PENDING,
    CANCELLED,
    COMPLETED
  }
}
