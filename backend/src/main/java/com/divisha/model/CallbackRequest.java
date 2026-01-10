package com.divisha.model;

import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "callbacks")
public class CallbackRequest {
  @Id private String id;
  private String requestId;
  private String patientName;
  private String phone;
  private String note;
  private LocalDateTime requestedAt;
  private String status;
  private Location location;
}
