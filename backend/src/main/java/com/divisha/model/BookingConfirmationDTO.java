package com.divisha.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmationDTO {
  private String patientName;
  private int age;
  private String gender;
  private String doctorName;
  private String location;
  private String appointmentDate;
  private String appointmentTime;
  private String consultationFee;
  private String patientId;
  private String bookingId;
  private String whatsappNumber;
  private String channelPhoneNumber;
}
