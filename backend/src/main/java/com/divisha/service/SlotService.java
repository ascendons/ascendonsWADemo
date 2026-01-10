package com.divisha.service;

import com.divisha.config.TimeZoneConfig;
import com.divisha.model.DoctorSchedule;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlotService {
  private final DoctorScheduleService doctorScheduleService;

  public List<String> getNextAvailableDates(DoctorSchedule schedule) {

    List<String> dates = new ArrayList<>();
    LocalDate today = LocalDate.now(TimeZoneConfig.ZONE_ID);
    LocalDate end = today.plusDays(90);

    for (LocalDate date = today; date.isBefore(end); date = date.plusDays(1)) {

      String day = date.getDayOfWeek().name();

      if (schedule.getUnavailableDaysOfWeek() != null
          && schedule.getUnavailableDaysOfWeek().contains(day)) continue;

      if (schedule.getUnavailableDates() != null
          && schedule.getUnavailableDates().contains(date.toString())) continue;

      dates.add(date.toString());
    }

    return dates;
  }

  public List<String> getNextAvailableDates(DoctorSchedule schedule, int days) {

    List<String> dates = new ArrayList<>();
    LocalDate today = LocalDate.now(TimeZoneConfig.ZONE_ID);
    LocalDate end = today.plusDays(days);

    for (LocalDate date = today; date.isBefore(end); date = date.plusDays(1)) {

      String day = date.getDayOfWeek().name();

      if (schedule.getUnavailableDaysOfWeek() != null
          && schedule.getUnavailableDaysOfWeek().contains(day)) continue;

      if (schedule.getUnavailableDates() != null
          && schedule.getUnavailableDates().contains(date.toString())) continue;

      dates.add(date.toString());
    }

    return dates;
  }

  public List<String> getSlotsByDate(String doctorId, String dateStr, String locationId) {
    DoctorSchedule schedule = doctorScheduleService.getDoctorScheduleByDoctorId(doctorId);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    LocalDate date = LocalDate.parse(dateStr, formatter);
    String day = date.getDayOfWeek().name();

    if (schedule.getUnavailableDaysOfWeek() != null
        && schedule.getUnavailableDaysOfWeek().contains(day)) return List.of();

    String dateIso = date.toString(); // Convert to ISO format (yyyy-MM-dd) for comparison
    if (schedule.getUnavailableDates() != null && schedule.getUnavailableDates().contains(dateIso))
      return List.of();

    Optional<DoctorSchedule.CustomDateSlot> custom =
        schedule.getCustomDateSlots() != null
            ? schedule.getCustomDateSlots().stream()
                .filter(c -> c.getDate().equals(dateIso))
                .findFirst()
            : Optional.empty();

    if (custom.isPresent()) {
      return generateTimes(
          custom.get().getStartTime(),
          custom.get().getEndTime(),
          custom.get().getSlotDurationMinutes());
    }
    return generateTimes(schedule.getStartTime(), schedule.getEndTime(), 15);
  }

  private List<String> generateTimes(String start, String end, int interval) {
    List<String> slots = new ArrayList<>();
    LocalTime s = LocalTime.parse(start);
    LocalTime e = LocalTime.parse(end);

    for (LocalTime t = s; t.isBefore(e); t = t.plusMinutes(interval)) {
      slots.add(t.toString());
    }
    return slots;
  }

  public List<String> getAvailableHoursForDate(DoctorSchedule schedule, String dateStr) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    LocalDate date = LocalDate.parse(dateStr, formatter);
    String day = date.getDayOfWeek().name();

    // Unavailable date or day skip
    if (schedule.getUnavailableDaysOfWeek() != null
        && schedule.getUnavailableDaysOfWeek().contains(day)) return List.of();

    String dateIso = date.toString(); // Convert to ISO format (yyyy-MM-dd) for comparison
    if (schedule.getUnavailableDates() != null && schedule.getUnavailableDates().contains(dateIso))
      return List.of();

    // Check custom slot override
    Optional<DoctorSchedule.CustomDateSlot> custom =
        schedule.getCustomDateSlots() != null
            ? schedule.getCustomDateSlots().stream()
                .filter(c -> c.getDate().equals(dateIso))
                .findFirst()
            : Optional.empty();

    String start =
        custom.map(DoctorSchedule.CustomDateSlot::getStartTime).orElse(schedule.getStartTime());
    String end =
        custom.map(DoctorSchedule.CustomDateSlot::getEndTime).orElse(schedule.getEndTime());

    LocalTime startTime = LocalTime.parse(start);
    LocalTime endTime = LocalTime.parse(end);

    List<String> hours = new ArrayList<>();
    for (LocalTime time = startTime; time.isBefore(endTime); time = time.plusHours(1)) {
      hours.add(time.toString().substring(0, 5)); // HH:mm only
    }

    return hours;
  }
}
