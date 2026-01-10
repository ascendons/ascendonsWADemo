package com.divisha.model;

import com.divisha.enums.Gender;
import java.time.Instant;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "patients")
public class Patient {
  @Id private String id;

  private String name;
  private String patientId;
  private Gender gender;
  private Integer age;
  private String notes;
  private Instant createdAt;
  private String phone;
}
