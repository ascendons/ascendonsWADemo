package com.divisha.controller;

import com.divisha.enums.AppointmentStatus;
import com.divisha.model.Appointment;
import com.divisha.service.AppointmentService;
import com.divisha.service.SlotService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {
  private final SlotService slotService;
  private final AppointmentService appointmentService;

  @GetMapping("/slots")
  public ResponseEntity<List<String>> getSlotsByDate(
      @RequestParam String date, @RequestParam String doctorId, @RequestParam String locationId) {
    List<String> slotsByDate =
        slotService.getSlotsByDate(doctorId, date.replace("-", ""), locationId);
    return ResponseEntity.ok(slotsByDate);
  }

  @PostMapping("/book")
  public ResponseEntity<Appointment> bookAppointment(@RequestBody Appointment appointment) {
    Appointment appointmentDto = appointmentService.bookAppointment(appointment);
    return ResponseEntity.ok(appointmentDto);
  }

  @PutMapping("/cancel")
  public ResponseEntity<String> cancelAppointment(@RequestParam String bookingId) {
    appointmentService.updateAppointmentStatus(
        bookingId, AppointmentStatus.CANCELLED.toString(), true);
    return ResponseEntity.ok("Appointment cancelled successfully");
  }

  @GetMapping("/byDate")
  public ResponseEntity<List<Appointment>> getAllAppointments(@RequestParam String date) {
    return ResponseEntity.ok(appointmentService.getAllAppointments(date.replace("-", "")));
  }

  @PutMapping("/reschedule")
  public ResponseEntity<Appointment> rescheduleAppointment(@RequestBody Appointment appointment) {
    Appointment appointmentDto = appointmentService.rescheduleAppointment(appointment);
    return ResponseEntity.ok(appointmentDto);
  }

  @PutMapping("/complete")
  public ResponseEntity<Appointment> completeAppointment(@RequestParam String id) {
    Appointment appointmentDto = appointmentService.completeAppointment(id);
    return ResponseEntity.ok(appointmentDto);
  }
}
