package com.divisha.model;

import com.divisha.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Doctor document. Keep canonical ID as _id; if you need an external code also store one but treat
 * _id as PK.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "doctors")
public class Doctor {
  @Id private String id;
  private String name;
  private String specialization;
  private Gender gender;
  private String status;
  private long createdAt;
}
