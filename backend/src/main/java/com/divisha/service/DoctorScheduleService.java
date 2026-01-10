package com.divisha.service;

import com.divisha.model.Doctor;
import com.divisha.model.DoctorSchedule;
import com.divisha.model.Location;
import com.divisha.repository.DoctorRepository;
import com.divisha.repository.DoctorScheduleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoctorScheduleService {
  private final DoctorScheduleRepository doctorScheduleRepository;
  private final DoctorRepository doctorRepository;
  private final LocationService locationService;

  public List<DoctorSchedule> getAllDoctorAvailableInThisLocation(String locationId) {
    return doctorScheduleRepository.findByLocationId(locationId);
  }

  public DoctorSchedule getDoctorScheduleByDoctorId(String doctorId) {
    List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorId(doctorId);
    return schedules.isEmpty() ? null : schedules.get(0);
  }

  public DoctorSchedule createDoctorSchedule(DoctorSchedule doctorSchedule) {
    List<DoctorSchedule> existing =
        doctorScheduleRepository.findByDoctorId(doctorSchedule.getDoctorId());
    if (!existing.isEmpty()) {
      throw new RuntimeException("Doctor Schedule already exists");
    }
    return doctorScheduleRepository.save(doctorSchedule);
  }

  public DoctorSchedule updateDoctorSchedule(DoctorSchedule doctorSchedule) {

    if (doctorSchedule == null || doctorSchedule.getDoctorId() == null) {
      throw new IllegalArgumentException("doctorSchedule and doctorId must be provided");
    }

    String doctorId = doctorSchedule.getDoctorId();

    List<DoctorSchedule> existingSchedules = doctorScheduleRepository.findByDoctorId(doctorId);
    DoctorSchedule existingSchedule = existingSchedules.isEmpty() ? null : existingSchedules.get(0);

    if (existingSchedule == null) {

      Doctor doctor =
          doctorRepository
              .findById(doctorId)
              .orElseThrow(
                  () -> new IllegalArgumentException("Doctor not found for id: " + doctorId));

      Location defaultLocation =
          locationService.getAllLocations().stream()
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("No locations available"));

      doctorSchedule.setDoctorName(doctor.getName());
      doctorSchedule.setLocationId(defaultLocation.getId());

      return doctorScheduleRepository.save(doctorSchedule);
    }

    existingSchedule.setStartTime(doctorSchedule.getStartTime());
    existingSchedule.setEndTime(doctorSchedule.getEndTime());
    existingSchedule.setUnavailableDaysOfWeek(doctorSchedule.getUnavailableDaysOfWeek());
    existingSchedule.setUnavailableDates(doctorSchedule.getUnavailableDates());

    if (doctorSchedule.getLocationId() != null) {
      existingSchedule.setLocationId(doctorSchedule.getLocationId());
    }

    return doctorScheduleRepository.save(existingSchedule);
  }
}
