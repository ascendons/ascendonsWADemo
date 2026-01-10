package com.divisha.model;

import com.divisha.enums.Gender;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Nullable
public class RegisterPatient {
  private String patientId;
  private String name;
  private String phone;
  @Nullable private Gender gender;
  @Nullable private Integer age;
  private String notes;
}
