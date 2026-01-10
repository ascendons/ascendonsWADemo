package com.divisha.flow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for appointment confirmation details displayed in the flow and sent to user after booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentConfirmationDTO {

  private String appointmentId;
  private String doctorName;
  private String locationName;
  private String date;
  private String timeSlot;
  private String status;
  private String confirmationMessage;
}
