package com.divisha.model;

import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "waitlist")
public class WaitlistEntry {
  @Id private String id;
  private String waitlistId;
  private String patientId;
  private String doctorId;
  private LocalDateTime requestedTime;
  private int waitWindowMinutes = 120;
  private String status;
  private String locationId;
}
