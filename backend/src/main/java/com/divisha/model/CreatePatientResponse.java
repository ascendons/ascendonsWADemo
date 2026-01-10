package com.divisha.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreatePatientResponse {
  private String message;
  private Patient patient;
}
