package com.divisha.controller;

import com.divisha.flow.dto.DropdownOption;
import com.divisha.flow.service.AppointmentFlowService;
import com.divisha.model.MetaFlowMessage;
import com.divisha.service.MetaMessageSender;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for sending WhatsApp Flow messages with dynamic data. This endpoint fetches real-time
 * data from the database and sends flows to users.
 */
@Slf4j
@RestController
@RequestMapping("/api/flows")
@RequiredArgsConstructor
public class FlowMessageController {

  private final AppointmentFlowService appointmentFlowService;
  private final MetaMessageSender messageSender;

  @Value("${meta.flow.appointment.booking.id}")
  private String appointmentFlowId;

  /**
   * Send appointment booking flow with dynamic doctor data
   *
   * @param phoneNumber WhatsApp phone number (with country code, no +)
   * @param patientId Patient ID for the appointment
   * @return Success response
   */
  @PostMapping("/send-appointment-flow")
  public ResponseEntity<Map<String, Object>> sendAppointmentFlow(
      @RequestParam String phoneNumber, @RequestParam String patientId) {

    log.info("📤 Sending appointment flow to: {} for patient: {}", phoneNumber, patientId);

    try {
      // Fetch dynamic doctor data from database
      List<DropdownOption> doctors = appointmentFlowService.getDoctorOptions();

      log.info("✅ Found {} doctors to populate in flow", doctors.size());

      // Prepare initial data for the flow
      Map<String, Object> initialData = new HashMap<>();
      initialData.put("doctors", doctors);
      initialData.put("locations", List.of());
      initialData.put("dates", List.of());
      initialData.put("time_slots", List.of());
      initialData.put("patient_id", patientId);
      initialData.put("phone_number", phoneNumber);

      // Build flow message
      MetaFlowMessage flowMessage =
          MetaFlowMessage.builder()
              .header("Book Your Appointment")
              .body(
                  "Welcome to Divisha Healthcare! Schedule your doctor appointment easily using our booking system.")
              .footer("Your health is our priority")
              .flowId(appointmentFlowId)
              .flowCta("Book Now")
              .flowAction("navigate")
              .screenId("APPOINTMENT")
              .flowActionPayload(initialData)
              .build();

      // Send flow message
      messageSender.sendFlow(phoneNumber, flowMessage);

      log.info("✅ Appointment flow sent successfully to {}", phoneNumber);

      return ResponseEntity.ok(
          Map.of(
              "success",
              true,
              "message",
              "Appointment flow sent successfully",
              "phoneNumber",
              phoneNumber,
              "patientId",
              patientId,
              "doctorsCount",
              doctors.size()));

    } catch (Exception e) {
      log.error("❌ Error sending appointment flow: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(Map.of("success", false, "error", e.getMessage(), "phoneNumber", phoneNumber));
    }
  }

  /**
   * Get doctors list (for testing)
   *
   * @return List of available doctors
   */
  @GetMapping("/doctors")
  public ResponseEntity<List<DropdownOption>> getDoctors() {
    List<DropdownOption> doctors = appointmentFlowService.getDoctorOptions();
    return ResponseEntity.ok(doctors);
  }

  /**
   * Get locations for a specific doctor (for testing)
   *
   * @param doctorId Doctor ID
   * @return List of locations where the doctor practices
   */
  @GetMapping("/locations/{doctorId}")
  public ResponseEntity<Map<String, Object>> getLocationsForDoctor(@PathVariable String doctorId) {
    List<DropdownOption> locations = appointmentFlowService.getLocationOptionsForDoctor(doctorId);
    return ResponseEntity.ok(
        Map.of(
            "doctorId", doctorId,
            "locations", locations,
            "count", locations.size()));
  }

  /**
   * Get available dates for a doctor at a location (for testing)
   *
   * @param doctorId Doctor ID
   * @param locationId Location ID
   * @return List of available dates
   */
  @GetMapping("/dates/{doctorId}/{locationId}")
  public ResponseEntity<Map<String, Object>> getAvailableDates(
      @PathVariable String doctorId, @PathVariable String locationId) {
    List<DropdownOption> dates = appointmentFlowService.getAvailableDates(doctorId, locationId);
    return ResponseEntity.ok(
        Map.of(
            "doctorId", doctorId,
            "locationId", locationId,
            "dates", dates,
            "count", dates.size()));
  }

  /**
   * Get available time slots for a specific date (for testing)
   *
   * @param doctorId Doctor ID
   * @param locationId Location ID
   * @param date Date in yyyy-MM-dd format
   * @return List of available time slots
   */
  @GetMapping("/time-slots/{doctorId}/{locationId}/{date}")
  public ResponseEntity<Map<String, Object>> getAvailableTimeSlots(
      @PathVariable String doctorId, @PathVariable String locationId, @PathVariable String date) {
    List<DropdownOption> timeSlots =
        appointmentFlowService.getAvailableTimeSlots(doctorId, locationId, date);
    return ResponseEntity.ok(
        Map.of(
            "doctorId", doctorId,
            "locationId", locationId,
            "date", date,
            "timeSlots", timeSlots,
            "count", timeSlots.size()));
  }
}
