package com.divisha.model;

import lombok.Data;

@Data
public class AppointmentRequest {

  private String slotId;
  private String patientId;
  private String phone;
  private String name;
  private String appointmentDate;
  private String appointmentTime;
}
