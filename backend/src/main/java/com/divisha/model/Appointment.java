package com.divisha.model;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Appointment links to slotId but also snapshots slot start time for auditability. We also index
 * and enforce uniqueness on slotId to prevent DB-level double-booking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "appointments")
public class Appointment {
  @Id private String id;
  private String doctorId;
  private String locationId;
  private String patientId;
  private String bookingId;
  private String date; // yyyyMMdd
  @Nullable private String time; // HH:MM
  private String phone;

  private String status;
  private long createdTs;
}
