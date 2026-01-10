package com.divisha.controller;

import com.divisha.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("/reports")
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportsController {

  private final AppointmentService appointmentService;

  @GetMapping("/total")
  public ResponseEntity<?> getReports(
      @RequestParam String startDate, @RequestParam String endDate) {
    return ResponseEntity.ok(appointmentService.getTotalAppointments(startDate, endDate));
  }

  @GetMapping("/non-cancelled")
  public ResponseEntity<?> getNonCancelledReports(
      @RequestParam String startDate, @RequestParam String endDate) {
    return ResponseEntity.ok(
        appointmentService.getTotalNonCancelledAppointments(startDate, endDate));
  }
}
