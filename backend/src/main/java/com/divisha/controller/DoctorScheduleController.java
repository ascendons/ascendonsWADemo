package com.divisha.controller;

import com.divisha.model.DoctorSchedule;
import com.divisha.service.DoctorScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorScheduleController {

  private final DoctorScheduleService doctorScheduleService;

  @GetMapping("/schedule")
  public ResponseEntity<DoctorSchedule> getAllDoctorSchedule(@RequestParam String doctorId) {
    return ResponseEntity.ok(doctorScheduleService.getDoctorScheduleByDoctorId(doctorId));
  }

  @PostMapping("/schedule")
  public ResponseEntity<DoctorSchedule> createDoctorSchedule(
      @RequestBody DoctorSchedule doctorSchedule) {
    return ResponseEntity.ok(doctorScheduleService.createDoctorSchedule(doctorSchedule));
  }

  @PutMapping("/schedule")
  public ResponseEntity<DoctorSchedule> updateDoctorSchedule(
      @RequestBody DoctorSchedule doctorSchedule) {
    return ResponseEntity.ok(doctorScheduleService.updateDoctorSchedule(doctorSchedule));
  }
}
