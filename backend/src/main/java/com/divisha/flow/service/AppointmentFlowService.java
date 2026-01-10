package com.divisha.flow.service;

import com.divisha.config.TimeZoneConfig;
import com.divisha.controller.FlowInfo;
import com.divisha.enums.AppointmentStatus;
import com.divisha.flow.dto.DropdownOption;
import com.divisha.flow.dto.FlowDataExchangeRequest;
import com.divisha.flow.dto.FlowDataExchangeResponse;
import com.divisha.model.*;
import com.divisha.repository.AppointmentRepository;
import com.divisha.repository.DoctorRepository;
import com.divisha.repository.DoctorScheduleRepository;
import com.divisha.repository.LocationRepository;
import com.divisha.service.MetaMessageSender;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to handle WhatsApp Flow data_exchange requests for appointment booking. Provides dynamic
 * dropdown options based on user selections.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentFlowService {

  private final DoctorRepository doctorRepository;
  private final LocationRepository locationRepository;
  private final DoctorScheduleRepository doctorScheduleRepository;
  private final AppointmentRepository appointmentRepository;
  private final com.divisha.repository.PatientRepository patientRepository;
  private final MetaMessageSender metaMessageSender;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter DISPLAY_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  private static final int DEFAULT_SLOT_DURATION_MINUTES = 15;
  private static final int DAYS_TO_SHOW = 100; // Show next 100 days

  @Value("${divisha.confirmation.fee:₹1200 (Payable at reception)}")
  private String feeText;

  /**
   * Handle data_exchange request from WhatsApp Flow. Routes to appropriate handler based on screen
   * and data.
   */
  public FlowDataExchangeResponse handleDataExchange(FlowDataExchangeRequest request) {
    log.info(
        "📊 Processing data_exchange for screen: {}, data: {}",
        request.getScreen(),
        request.getData());

    try {
      String requestFlowToken = request.getFlowToken();
      FlowInfo info = parseFlowString(requestFlowToken);
      Map<String, Object> data = request.getData();
      String screen = request.getScreen();
      String trigger = (String) data.get("trigger");

      log.info("🎯 Trigger: {}", trigger);

      // Build response data based on screen and trigger
      Map<String, Object> responseData = new HashMap<>();

      if ("APPOINTMENT".equals(screen)) {
        handleAppointmentScreen(data, trigger, responseData, info);
      } else if ("CONFIRMATION".equals(screen)) {
        handleConfirmationScreen(data, responseData, info);
      }

      // Determine which screen to show
      String targetScreen = request.getScreen(); // Default to current screen
      String triggerValue = (String) data.get("trigger");

      log.info("🔍 Screen: {}, Trigger: {}", request.getScreen(), triggerValue);

      // Navigate based on trigger
      if ("APPOINTMENT".equals(request.getScreen())
          && "prepare_confirmation".equals(triggerValue)) {
        targetScreen = "CONFIRMATION"; // Navigate to confirmation screen
        log.info("✅ Navigating to CONFIRMATION screen with prepared data");
      } else if ("CONFIRMATION".equals(request.getScreen())
          && "confirm_booking".equals(triggerValue)) {
        targetScreen = "SUCCESS"; // Navigate to success screen after booking
        log.info("✅ Navigating to SUCCESS screen after booking confirmation");
      }

      log.info("📤 Returning screen: {}, data keys: {}", targetScreen, responseData.keySet());

      return FlowDataExchangeResponse.builder()
          .version(request.getVersion())
          .screen(targetScreen)
          .data(responseData)
          .build();

    } catch (Exception e) {
      log.error("❌ Error processing data_exchange: {}", e.getMessage(), e);
      return FlowDataExchangeResponse.builder()
          .version(request.getVersion())
          .screen(request.getScreen())
          .errorMessage("Failed to load data. Please try again.")
          .build();
    }
  }

  /** Handle APPOINTMENT screen data exchange */
  private void handleAppointmentScreen(
      Map<String, Object> data, String trigger, Map<String, Object> responseData, FlowInfo info) {

    String doctorId = (String) data.get("doctor");
    String locationId = (String) data.get("location");
    String dateStr = (String) data.get("date");

    // Get patient_id and phone_number from data payload (preserved through all exchanges)
    String patientId = (String) data.get("patient_id");
    String phoneNumber = (String) data.get("phone_number");

    // Fallback to FlowInfo if not in data (for initial load)
    if (patientId == null) {
      patientId = info.patientId();
    }
    if (phoneNumber == null) {
      phoneNumber = info.phoneNumber();
    }

    // Always provide doctors list
    responseData.put("doctors", getDoctorOptions());

    // Preserve patient_id and phone_number through all data exchanges
    if (patientId != null) {
      responseData.put("patient_id", patientId);
    }
    if (phoneNumber != null) {
      responseData.put("phone_number", phoneNumber);
    }

    if ("prepare_confirmation".equals(trigger)) {
      // User clicked "Continue" - prepare confirmation screen
      log.info("📋 Preparing confirmation screen");
      log.info("   Patient ID: {}", patientId);
      log.info("   Doctor ID: {}", doctorId);
      log.info("   Location ID: {}", locationId);
      log.info("   Date: {}", dateStr);
      log.info("   Time Slot: {}", data.get("time_slot"));

      String doctorName = getDoctorNameById(doctorId);
      String locationName = getLocationNameById(locationId);
      String patientName = getPatientNameById(patientId);
      String displayDate = formatDateForDisplay(dateStr);

      // Pre-generate booking ID
      String bookingId = generateBookingId();

      // Get time slot value
      String timeSlot = (String) data.get("time_slot");

      log.info("   Doctor Name: {}", doctorName);
      log.info("   Location Name: {}", locationName);
      log.info("   Patient Name: {}", patientName);
      log.info("   Display Date: {}", displayDate);
      log.info("   Booking ID: {}", bookingId);

      // Build detailed summary
      String summary =
          String.format(
              "👤 Patient: %s\n"
                  + "🩺 Doctor: %s\n"
                  + "📍 Location: %s\n"
                  + "📅 Date: %s\n"
                  + "⏰ Time: %s\n"
                  + "🔖 Booking ID: %s",
              patientName, doctorName, locationName, displayDate, timeSlot, bookingId);

      log.info("✅ Confirmation summary built successfully");
      log.info("📝 Summary: {}", summary);

      // Set all data needed for confirmation screen
      responseData.put("appointment_summary", summary);
      responseData.put("doctor", doctorId);
      responseData.put("location", locationId);
      responseData.put("date", dateStr);
      responseData.put("time_slot", data.get("time_slot")); // Use actual time slot from form
      responseData.put("booking_id", bookingId);
      responseData.put("patient_name", patientName);
      responseData.put("patient_id", patientId);
      responseData.put("phone_number", phoneNumber);

      log.info("✅ Response data keys: {}", responseData.keySet());

    } else if (trigger == null || "doctor_selected".equals(trigger)) {
      // Initial load or doctor just selected
      log.info("📋 Doctor selection trigger - doctorId: {}", doctorId);

      if (doctorId != null && !doctorId.isEmpty()) {
        List<DropdownOption> locations = getLocationOptionsForDoctor(doctorId);
        responseData.put("locations", locations);
        responseData.put("is_location_enabled", !locations.isEmpty());

        log.info(
            "✅ Locations for doctor {}: {} locations, enabled: {}",
            doctorId,
            locations.size(),
            !locations.isEmpty());
      } else {
        responseData.put("locations", new ArrayList<>());
        responseData.put("is_location_enabled", false);
        log.info("⚠️ No doctor selected, locations disabled");
      }
      responseData.put("dates", new ArrayList<>());
      responseData.put("is_date_enabled", false);
      responseData.put("time_slots", new ArrayList<>());
      responseData.put("is_time_enabled", false);

    } else if ("location_selected".equals(trigger)) {
      // Location just selected
      log.info(
          "📍 Location selection trigger - doctorId: {}, locationId: {}", doctorId, locationId);

      List<DropdownOption> locations = getLocationOptionsForDoctor(doctorId);
      responseData.put("locations", locations);
      responseData.put("is_location_enabled", true);

      if (locationId != null && !locationId.isEmpty()) {
        List<DropdownOption> dates = getAvailableDates(doctorId, locationId);
        responseData.put("dates", dates);
        responseData.put("is_date_enabled", !dates.isEmpty());

        log.info(
            "✅ Dates for doctor {} at location {}: {} dates, enabled: {}",
            doctorId,
            locationId,
            dates.size(),
            !dates.isEmpty());
      } else {
        responseData.put("dates", new ArrayList<>());
        responseData.put("is_date_enabled", false);
        log.info("⚠️ No location selected, dates disabled");
      }
      responseData.put("time_slots", new ArrayList<>());
      responseData.put("is_time_enabled", false);

    } else if ("date_selected".equals(trigger)) {
      // Date just selected
      log.info(
          "📅 Date selection trigger - doctorId: {}, locationId: {}, dateStr: {}",
          doctorId,
          locationId,
          dateStr);

      List<DropdownOption> locations = getLocationOptionsForDoctor(doctorId);
      responseData.put("locations", locations);
      responseData.put("is_location_enabled", true);

      List<DropdownOption> dates = getAvailableDates(doctorId, locationId);
      responseData.put("dates", dates);
      responseData.put("is_date_enabled", true);

      if (dateStr != null && !dateStr.isEmpty()) {
        List<DropdownOption> timeSlots = getAvailableTimeSlots(doctorId, locationId, dateStr);
        responseData.put("time_slots", timeSlots);
        responseData.put("is_time_enabled", !timeSlots.isEmpty());

        log.info(
            "✅ Time slots for date {}: {} slots, enabled: {}",
            dateStr,
            timeSlots.size(),
            !timeSlots.isEmpty());
      } else {
        responseData.put("time_slots", new ArrayList<>());
        responseData.put("is_time_enabled", false);
        log.info("⚠️ No date selected, time slots disabled");
      }
    }
  }

  /** Handle CONFIRMATION screen data exchange - create appointment and return to SUCCESS screen */
  private void handleConfirmationScreen(
      Map<String, Object> data, Map<String, Object> responseData, FlowInfo info) {

    String trigger = (String) data.get("trigger");

    // Get patient_id and phone_number from data payload
    String patientId = (String) data.get("patient_id");
    String phoneNumber = (String) data.get("phone_number");

    // Fallback to FlowInfo if not in data
    if (patientId == null) {
      patientId = info.patientId();
    }
    if (phoneNumber == null) {
      phoneNumber = info.phoneNumber();
    }

    log.info("📋 CONFIRMATION screen - Patient ID: {}, Phone: {}", patientId, phoneNumber);

    if ("confirm_booking".equals(trigger)) {
      // Create the appointment
      String doctorId = (String) data.get("doctor");
      String locationId = (String) data.get("location");
      String dateStr = (String) data.get("date");
      String timeSlot = (String) data.get("time_slot");
      String preGeneratedBookingId = (String) data.get("booking_id");

      log.info(
          "🎯 Creating appointment - Patient: {}, Doctor: {}, Location: {}, Date: {}, Time: {}",
          patientId,
          doctorId,
          locationId,
          dateStr,
          timeSlot);

      // Use phone number from data or fallback to "unknown"
      String phone = phoneNumber != null ? phoneNumber : "unknown";

      Appointment appointment = createAppointment(data, phone, preGeneratedBookingId);

      // Get names for display
      String doctorName = getDoctorNameById(doctorId);
      String locationName = getLocationNameById(locationId);
      String patientName = getPatientNameById(patientId);
      String displayDate = formatDateForDisplay(dateStr);

      // Build confirmation message based on appointment status
      String confirmationMessage;
      if (AppointmentStatus.WAITLISTED.name().equals(appointment.getStatus())) {
        confirmationMessage =
            String.format(
                """
                            ⏳ You have been added to the Waiting list!

                            👤 Patient: %s
                            🔖 Booking ID: %s
                            🩺 Doctor: %s
                            📍 Location: %s
                            📅 Date: %s
                            ⏰ Time: %s

                            ⚠️ This time slot is currently booked. We will notify you if it becomes available.
                    """,
                patientName,
                appointment.getBookingId(),
                doctorName,
                locationName,
                displayDate,
                timeSlot);
      } else {
        confirmationMessage =
            String.format(
                """
                            ✅ Your appointment has been confirmed!

                            👤 Patient: %s
                            🔖 Booking ID: %s
                            🩺 Doctor: %s
                            📍 Location: %s
                            📅 Date: %s
                            ⏰ Time: %s
                            💰 Consultation Fee: %s

                            We look forward to seeing you!""",
                patientName,
                appointment.getBookingId(),
                doctorName,
                locationName,
                displayDate,
                timeSlot,
                feeText);
      }

      responseData.put("booking_id", appointment.getBookingId());
      responseData.put("confirmation_message", confirmationMessage);
      responseData.put("appointment_status", appointment.getStatus());

      log.info(
          "✅ Appointment created: {}, Status: {}",
          appointment.getBookingId(),
          appointment.getStatus());
      log.info("📝 SUCCESS screen data:");
      log.info("   Booking ID: {}", appointment.getBookingId());
      log.info("   Status: {}", appointment.getStatus());
      log.info("   Confirmation Message: {}", confirmationMessage);
      log.info("   Response data keys: {}", responseData.keySet());
      metaMessageSender.sendText(
          phoneNumber, MetaTextMessage.builder().text(confirmationMessage).build());
    } else {
      // Just showing confirmation screen, build summary
      String doctorId = (String) data.get("doctor");
      String locationId = (String) data.get("location");
      String dateStr = (String) data.get("date");
      String timeSlot = (String) data.get("time_slot");

      log.info("📋 Building confirmation summary for patient: {}", patientId);

      String doctorName = getDoctorNameById(doctorId);
      String locationName = getLocationNameById(locationId);
      String patientName = getPatientNameById(patientId);
      String displayDate = formatDateForDisplay(dateStr);

      // Pre-generate booking ID to show on confirmation screen
      String bookingId = generateBookingId();

      // Build detailed summary
      String summary =
          String.format(
              "👤 Patient: %s\n"
                  + "🩺 Doctor: %s\n"
                  + "📍 Location: %s\n"
                  + "📅 Date: %s\n"
                  + "⏰ Time: %s\n"
                  + "🔖 Booking ID: %s",
              patientName, doctorName, locationName, displayDate, timeSlot, bookingId);

      log.info("✅ Confirmation summary built: {}", summary);

      responseData.put("appointment_summary", summary);
      responseData.put("doctor", doctorId);
      responseData.put("location", locationId);
      responseData.put("date", dateStr);
      responseData.put("time_slot", timeSlot);
      responseData.put("booking_id", bookingId);
      responseData.put("patient_name", patientName);

      // Preserve patient_id and phone_number for the final confirmation
      if (patientId != null) {
        responseData.put("patient_id", patientId);
      }
      if (phoneNumber != null) {
        responseData.put("phone_number", phoneNumber);
      }
    }
  }

  /** Get patient name by ID */
  private String getPatientNameById(String patientId) {
    if (patientId == null) {
      return "Unknown Patient";
    }
    try {
      com.divisha.model.Patient patient = patientRepository.findByPatientId(patientId);
      return patient != null ? patient.getName() : "Unknown Patient";
    } catch (Exception e) {
      log.warn("⚠️ Failed to fetch patient name for ID: {}", patientId);
      return "Unknown Patient";
    }
  }

  /** Format date from yyyy-MM-dd to readable format */
  private String formatDateForDisplay(String dateStr) {
    try {
      LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
      return date.format(DISPLAY_DATE_FORMATTER);
    } catch (Exception e) {
      log.warn("⚠️ Failed to format date: {}", dateStr);
      return dateStr;
    }
  }

  /** Get doctor name by ID */
  private String getDoctorNameById(String doctorId) {
    return doctorRepository
        .findById(doctorId)
        .map(d -> d.getName() + " - " + d.getSpecialization())
        .orElse("Doctor");
  }

  /** Get location name by ID */
  private String getLocationNameById(String locationId) {
    return locationRepository.findById(locationId).map(Location::getName).orElse("Location");
  }

  /** Get list of all active doctors as dropdown options. */
  public List<DropdownOption> getDoctorOptions() {
    List<Doctor> doctors =
        doctorRepository.findAll().stream()
            .filter(d -> "active".equalsIgnoreCase(d.getStatus()))
            .collect(Collectors.toList());

    log.info("Found {} active doctors", doctors.size());

    return doctors.stream()
        .map(
            doctor ->
                DropdownOption.builder()
                    .id(doctor.getId())
                    .title(doctor.getName())
                    .description(doctor.getSpecialization())
                    .build())
        .collect(Collectors.toList());
  }

  /** Get locations where a specific doctor practices. */
  public List<DropdownOption> getLocationOptionsForDoctor(String doctorId) {
    log.info("🔍 Fetching locations for doctor: {}", doctorId);

    // Get all schedules for this doctor
    List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorId(doctorId);

    if (schedules.isEmpty()) {
      log.warn("⚠️ No schedules found for doctor: {}", doctorId);
      return Collections.emptyList();
    }

    log.info("📋 Found {} schedules for doctor: {}", schedules.size(), doctorId);

    // Get unique location IDs
    Set<String> locationIds =
        schedules.stream().map(DoctorSchedule::getLocationId).collect(Collectors.toSet());

    log.info("📍 Unique location IDs for doctor {}: {}", doctorId, locationIds);

    // Fetch location details
    List<Location> locations =
        locationRepository.findAllById(locationIds).stream()
            .filter(l -> "true".equalsIgnoreCase(l.getStatus()))
            .toList();

    log.info("✅ Found {} active locations for doctor: {}", locations.size(), doctorId);

    List<DropdownOption> dropdownOptions =
        locations.stream()
            .map(
                location ->
                    DropdownOption.builder()
                        .id(location.getId())
                        .title(location.getName())
                        .description(location.getAddress())
                        .build())
            .collect(Collectors.toList());

    log.info("📤 Returning {} location options: {}", dropdownOptions.size(), dropdownOptions);

    return dropdownOptions;
  }

  /** Get available dates for a doctor at a specific location. */
  public List<DropdownOption> getAvailableDates(String doctorId, String locationId) {
    List<DoctorSchedule> schedules =
        doctorScheduleRepository.findByDoctorIdAndLocationId(doctorId, locationId);

    if (schedules.isEmpty()) {
      log.warn("No schedule found for doctor: {} at location: {}", doctorId, locationId);
      return Collections.emptyList();
    }

    DoctorSchedule schedule = schedules.get(0);
    List<DropdownOption> availableDates = new ArrayList<>();
    LocalDate today = LocalDate.now(TimeZoneConfig.ZONE_ID);

    for (int i = 0; i <= DAYS_TO_SHOW; i++) {
      LocalDate date = today.plusDays(i);
      if (isDateAvailable(date, schedule)) {
        String dateStr = date.format(DATE_FORMATTER);
        String displayDate = date.format(DISPLAY_DATE_FORMATTER);

        availableDates.add(
            DropdownOption.builder().id(dateStr).title(displayDate).description("").build());
      }
    }

    log.info("Found {} available dates", availableDates.size());
    return availableDates;
  }

  /** Get available time slots for a specific date. */
  public List<DropdownOption> getAvailableTimeSlots(
      String doctorId, String locationId, String dateStr) {
    List<DoctorSchedule> schedules =
        doctorScheduleRepository.findByDoctorIdAndLocationId(doctorId, locationId);

    if (schedules.isEmpty()) {
      log.warn(
          "No schedule found for doctor: {} at location: {} on date: {}",
          doctorId,
          locationId,
          dateStr);
      return Collections.emptyList();
    }

    DoctorSchedule schedule = schedules.get(0);
    LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);

    // Check for custom slots on this date
    Optional<DoctorSchedule.CustomDateSlot> customSlot =
        schedule.getCustomDateSlots() != null
            ? schedule.getCustomDateSlots().stream()
                .filter(cs -> dateStr.equals(cs.getDate()))
                .findFirst()
            : Optional.empty();

    String startTimeStr;
    String endTimeStr;
    int slotDuration;

    if (customSlot.isPresent()) {
      startTimeStr = customSlot.get().getStartTime();
      endTimeStr = customSlot.get().getEndTime();
      slotDuration =
          customSlot.get().getSlotDurationMinutes() != null
              ? customSlot.get().getSlotDurationMinutes()
              : DEFAULT_SLOT_DURATION_MINUTES;
    } else {
      startTimeStr = schedule.getStartTime();
      endTimeStr = schedule.getEndTime();
      slotDuration = DEFAULT_SLOT_DURATION_MINUTES;
    }

    LocalTime startTime = LocalTime.parse(startTimeStr, TIME_FORMATTER);
    LocalTime endTime = LocalTime.parse(endTimeStr, TIME_FORMATTER);

    // Get already booked appointments for this date
    String dateFormatted = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    List<Appointment> bookedAppointments =
        appointmentRepository.findByDoctorIdAndLocationIdAndDate(
            doctorId, locationId, dateFormatted);

    Set<String> bookedTimes =
        bookedAppointments.stream()
            .filter(a -> !AppointmentStatus.CANCELLED.name().equalsIgnoreCase(a.getStatus()))
            .map(Appointment::getTime)
            .collect(Collectors.toSet());

    // Calculate minimum bookable time for today (current time + 1 hour)
    LocalDate today = LocalDate.now(TimeZoneConfig.ZONE_ID);
    LocalTime minimumBookableTime = null;

    log.info(
        "🔍 Date comparison - Selected date: {}, Today: {}, Are equal: {}",
        date,
        today,
        date.equals(today));

    if (date.equals(today)) {
      LocalTime currentTime = LocalTime.now(TimeZoneConfig.ZONE_ID);
      minimumBookableTime = currentTime.plusHours(1);
      log.info(
          "📅 Selected date is TODAY. Current time: {}, Minimum bookable time: {}",
          currentTime,
          minimumBookableTime);
    } else {
      log.info("📅 Selected date is NOT today. No time filtering will be applied.");
    }

    // Generate time slots
    List<DropdownOption> timeSlots = new ArrayList<>();
    LocalTime currentSlot = startTime;
    int skippedCount = 0;

    while (currentSlot.isBefore(endTime)) {
      String timeStr = currentSlot.format(TIME_FORMATTER);

      // Skip slots that are less than 1 hour from now for today's date
      if (minimumBookableTime != null && currentSlot.isBefore(minimumBookableTime)) {
        log.debug(
            "⏭️ Skipping slot {} (before minimum bookable time {})",
            currentSlot,
            minimumBookableTime);
        skippedCount++;
        currentSlot = currentSlot.plusMinutes(slotDuration);
        continue;
      }

      if (!bookedTimes.contains(timeStr)) {
        String displayTime = currentSlot.format(DateTimeFormatter.ofPattern("hh:mm a"));
        timeSlots.add(
            DropdownOption.builder()
                .id(timeStr)
                .title(displayTime)
                .description("Available")
                .build());
      }

      if (bookedTimes.contains(timeStr)) {
        String displayTime = currentSlot.format(DateTimeFormatter.ofPattern("hh:mm a"));
        timeSlots.add(
            DropdownOption.builder()
                .id(timeStr)
                .title(displayTime)
                .description("NA (Join WaitList)")
                .build());
      }

      currentSlot = currentSlot.plusMinutes(slotDuration);
    }

    log.info(
        "Found {} time slots for date: {} (skipped {} past slots)",
        timeSlots.size(),
        dateStr,
        skippedCount);
    return timeSlots;
  }

  /** Check if a date is available based on doctor's schedule. */
  private boolean isDateAvailable(LocalDate date, DoctorSchedule schedule) {
    // Check if date is in unavailable dates list
    if (schedule.getUnavailableDates() != null) {
      String dateStr = date.format(DATE_FORMATTER);
      if (schedule.getUnavailableDates().contains(dateStr)) {
        return false;
      }
    }

    // Check if day of week is unavailable
    if (schedule.getUnavailableDaysOfWeek() != null) {
      DayOfWeek dayOfWeek = date.getDayOfWeek();
      if (schedule.getUnavailableDaysOfWeek().contains(dayOfWeek.toString())) {
        return false;
      }
    }

    return true;
  }

  /**
   * Create appointment booking from flow completion data with optional pre-generated booking ID.
   */
  public Appointment createAppointment(
      Map<String, Object> flowData, String phoneNumber, String bookingId) {
    String doctorId = (String) flowData.get("doctor");
    String locationId = (String) flowData.get("location");
    String dateStr = (String) flowData.get("date");
    String timeStr = (String) flowData.get("time_slot");
    String patientId = (String) flowData.get("patient_id");

    // Convert date format yyyy-MM-dd to yyyyMMdd
    LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
    String dateFormatted = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    // Use provided booking ID or generate new one
    String finalBookingId =
        (bookingId != null && !bookingId.isEmpty()) ? bookingId : generateBookingId();

    log.info("📋 Creating appointment with booking ID: {}", finalBookingId);

    // Check if the time slot is already booked
    List<Appointment> existingAppointments =
        appointmentRepository.findByDoctorIdAndLocationIdAndDate(
            doctorId, locationId, dateFormatted);

    boolean isSlotBooked =
        existingAppointments.stream()
            .filter(a -> !AppointmentStatus.CANCELLED.name().equalsIgnoreCase(a.getStatus()))
            .anyMatch(a -> timeStr.equals(a.getTime()));

    // Set status based on slot availability
    String appointmentStatus =
        isSlotBooked ? AppointmentStatus.WAITLISTED.name() : AppointmentStatus.CONFIRMED.name();

    log.info(
        "📊 Slot status - Time: {}, IsBooked: {}, Status: {}",
        timeStr,
        isSlotBooked,
        appointmentStatus);

    Appointment appointment =
        Appointment.builder()
            .doctorId(doctorId)
            .locationId(locationId)
            .patientId(patientId)
            .date(dateFormatted)
            .time(timeStr)
            .phone(phoneNumber)
            .status(appointmentStatus)
            .createdTs(System.currentTimeMillis())
            .bookingId(finalBookingId)
            .build();
    if (appointmentRepository.existsAppointmentByBookingId(finalBookingId)) {
      log.info("Appointment already exists for this booking id : {}", finalBookingId);
      appointment.setBookingId(generateBookingId());
    }

    Appointment saved = appointmentRepository.save(appointment);
    log.info(
        "Appointment created with ID: {}, Status: {}", saved.getBookingId(), saved.getStatus());
    return saved;
  }

  /** Generate unique booking ID. */
  private String generateBookingId() {
    return "BID-" + System.currentTimeMillis() / 1000L;
  }

  public static FlowInfo parseFlowString(String input) {
    String regex = "^FLOW_(.+?)_(.+)$";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(input);

    if (matcher.find()) {
      return new FlowInfo(
          matcher.group(1), // phoneNumber
          matcher.group(2) // patientId
          );
    } else {
      throw new IllegalArgumentException("Invalid FLOW string format: " + input);
    }
  }
}
