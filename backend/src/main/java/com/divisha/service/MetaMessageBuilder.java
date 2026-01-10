package com.divisha.service;

import com.divisha.model.*;
import com.divisha.repository.PatientRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MetaMessageBuilder {
  private final PatientRepository patientRepository;

  // -------------------------------------------------------------------------
  // COMMON HELPERS
  // -------------------------------------------------------------------------

  private MetaInteractiveButtonMessage buttonMessage(
      String header, String body, String footer, List<MetaButtonOption> buttons) {
    return MetaInteractiveButtonMessage.builder()
        .header(header)
        .body(body)
        .footer(footer)
        .buttons(buttons)
        .build();
  }

  private MetaInteractiveListMessage listMessage(
      String header,
      String body,
      String footer,
      String buttonText,
      String sectionTitle,
      List<MetaListRow> rows) {
    return MetaInteractiveListMessage.builder()
        .header(header)
        .body(body)
        .footer(footer)
        .buttonText(buttonText)
        .sectionTitle(sectionTitle)
        .rows(rows)
        .build();
  }

  private MetaButtonOption btn(String id, String title) {
    return new MetaButtonOption(id, title);
  }

  private MetaListRow row(String id, String title, String desc) {
    return new MetaListRow(id, title, desc);
  }

  // -------------------------------------------------------------------------
  // 1. APPOINTMENT MAIN MENU
  // -------------------------------------------------------------------------

  public MetaInteractiveButtonMessage appointmentMenu() {

    String bodyText =
        """
                Welcome to Ascendons Appointment.
                Thank you for choosing us for your healthcare needs.
                Please let us know how we can assist you today:
                """;

    List<MetaButtonOption> buttons =
        List.of(
            btn("BOOK_APPOINTMENT", "BOOK APPOINTMENT"),
            btn("CANCEL_APPOINTMENT", "CANCEL/RESCHEDULE"),
            btn("RESCHEDULE", "WALK-IN/EMERGENCY"));

    return buttonMessage(
        "Ascendons Appointment", bodyText, "Please select an option:", buttons);
  }

  public MetaInteractiveButtonMessage appointmentCancelRescheduleMenu() {

    String bodyText =
        """
                Welcome to Ascendons Appointment.
                Thank you for choosing us for your healthcare needs.
                Please let us know how we can assist you today:
                """;

    List<MetaButtonOption> buttons =
        List.of(btn("BOOK_APPOINTMENT", "CANCEL"), btn("CANCEL_APPOINTMENT", "RESCHEDULE"));
    return buttonMessage(
        "Ascendons Appointment", bodyText, "Please select an option:", buttons);
  }

  public MetaInteractiveButtonMessage top3AvailableDates(List<String> availableDates) {

    String bodyText =
        """
                We understand your urgency please select a day from the below, if you want to opt some other days go with the regular Book Appointment option.
                Please let us know how we can assist you today:
                """;

    List<MetaButtonOption> buttons =
        List.of(
            btn("DATE_" + availableDates.get(0), availableDates.get(0)),
            btn("DATE_" + availableDates.get(1), availableDates.get(1)),
            btn("DATE_" + availableDates.get(2), availableDates.get(2)));
    return buttonMessage(
        "Ascendons Appointment", bodyText, "Please select an option:", buttons);
  }

  // -------------------------------------------------------------------------
  // 2. FOLLOW-UP MENU
  // -------------------------------------------------------------------------

  public MetaInteractiveButtonMessage followUpMenu(List<Patient> patientList) {

    List<MetaButtonOption> buttons = new ArrayList<>();

    for (Patient patient : patientList) {
      buttons.add(btn("PAT_" + patient.getId(), patient.getName()));
    }

    if (patientList.size() < 3) {
      buttons.add(btn("ADD_NEW_PATIENT", "Add New Patient"));
    }

    return buttonMessage(
        "Select Patient",
        "We found your records. Please select a patient:",
        "Tap to continue",
        buttons);
  }

  // -------------------------------------------------------------------------
  // 3. LOCATION MENU
  // -------------------------------------------------------------------------

  public MetaInteractiveButtonMessage locationMenu(List<Location> locations) {

    List<MetaButtonOption> buttons = new ArrayList<>();

    locations.stream()
        .limit(4)
        .forEach(loc -> buttons.add(btn("LOC_" + loc.getId(), loc.getName())));

    return buttonMessage(
        "Ascendons Appointment",
        "Please select your preferred location:",
        "Please select any one option:",
        buttons);
  }

  public MetaInteractiveListMessage availableDoctorList(List<DoctorSchedule> doctorSchedules) {

    List<MetaListRow> rows =
        doctorSchedules.stream()
            .map(doc -> row("DOC_" + doc.getDoctorId(), doc.getDoctorName(), ""))
            .toList();

    return listMessage(
        "Select Doctor",
        "Please select the doctor you want to consult with:",
        "Choose a doctor",
        "Select Doctor",
        "Available Doctors",
        rows);
  }

  // -------------------------------------------------------------------------
  // 6. CONFIRMATION BUTTONS
  // -------------------------------------------------------------------------

  public MetaInteractiveButtonMessage confirmationButtons(BookingConfirmationDTO dto) {

    String body =
        """
                Please check your booking details:

                👤 *Name:* %s
                🩺 *Doctor:* %s
                📍 *Location:* %s
                🕒 *Slot:* %s at %s
                💰 *Consultation Fee:* %s
                🆔 *Patient ID:* %s

                Thank you for choosing Ascendons Appointment 🙏
                """
            .formatted(
                dto.getPatientName(),
                dto.getDoctorName(),
                dto.getLocation(),
                dto.getAppointmentDate(),
                dto.getAppointmentTime(),
                dto.getConsultationFee(),
                dto.getPatientId());

    List<MetaButtonOption> buttons = List.of(btn("CONFIRM", "CONFIRM"), btn("CANCEL", "CANCEL"));

    return buttonMessage(
        "Ascendons Appointment", body, "Please select any one option:", buttons);
  }

  // -------------------------------------------------------------------------
  // 7. TEXT RESPONSES
  // -------------------------------------------------------------------------

  public String confirmationSuccessText(BookingConfirmationDTO dto, String successText) {
    return """
                ✅ %s

                👤 *Name:* %s
                🩺 *Doctor:* %s
                📍 *Location:* %s
                📅 *Date:* %s
                🕒 *Time:* %s
                💰 *Consultation Fee:* %s
                🆔 *Patient ID:* %s
                🔖 *Booking ID:* %s

                Thank you for choosing Ascendons Appointment 🙏
                """
        .formatted(
            successText,
            dto.getPatientName(),
            dto.getDoctorName(),
            dto.getLocation(),
            dto.getAppointmentDate(),
            dto.getAppointmentTime(),
            dto.getConsultationFee(),
            dto.getPatientId(),
            dto.getBookingId());
  }

  public String confirmationWaitListSuccessText(BookingConfirmationDTO dto) {
    return """
            🕒 *Added to Waiting List*

            👤 *Name:* %s
            🩺 *Doctor:* %s
            📍 *Location:* %s
            📅 *Date:* %s
            🕒 *Preferred Time:* %s
            💰 *Consultation Fee:* %s
            🆔 *Patient ID:* %s
            🔖 *Request ID:* %s

            Thank you for choosing *Ascendons Appointment* 🙏
            We will notify you as soon as a slot becomes available.
            """
        .formatted(
            dto.getPatientName(),
            dto.getDoctorName(),
            dto.getLocation(),
            dto.getAppointmentDate(),
            dto.getAppointmentTime(),
            dto.getConsultationFee(),
            dto.getPatientId(),
            dto.getBookingId());
  }

  // -------------------------------------------------------------------------
  // 8. DATE RANGE LIST
  // -------------------------------------------------------------------------

  public MetaInteractiveListMessage nextMonthRanges(List<String> nextAvailableDates) {

    DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd MMM");
    DateTimeFormatter payloadFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    List<LocalDate> dates = nextAvailableDates.stream().map(LocalDate::parse).sorted().toList();

    List<MetaListRow> rows = new ArrayList<>();
    int group = 9;

    for (int i = 0; i < dates.size(); i += group) {

      LocalDate start = dates.get(i);
      LocalDate end = dates.get(Math.min(i + group - 1, dates.size() - 1));

      rows.add(
          row(
              "DATE_%s_TO_%s".formatted(payloadFmt.format(start), payloadFmt.format(end)),
              "%s to %s".formatted(labelFmt.format(start), labelFmt.format(end)),
              ""));
    }

    return listMessage(
        "Appointment Booking",
        "Please choose a date range for your appointment:",
        "Select date range",
        "Select Date Range",
        "Available Date Ranges",
        rows);
  }

  // -------------------------------------------------------------------------
  // 9. 2-HOUR SLOTS
  // -------------------------------------------------------------------------

  public MetaInteractiveListMessage twoHourSlots(
      String selectedDate, List<String> availableHourSlot) {

    DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("HH:mm");
    DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("hh:mm a");

    List<MetaListRow> rows =
        availableHourSlot.stream()
            .map(
                slot -> {
                  LocalTime start = LocalTime.parse(slot, inputFmt);
                  LocalTime end = start.plusHours(1);

                  return row(
                      "TIME_%s_%s-%s"
                          .formatted(
                              selectedDate.replace("-", ""),
                              start.toString().substring(0, 2),
                              end.toString().substring(0, 2)),
                      "%s - %s".formatted(outFmt.format(start), outFmt.format(end)),
                      "");
                })
            .toList();

    return listMessage(
        "Select Time Slot",
        "Please choose your preferred appointment time:",
        "Choose time",
        "Select Time Slot",
        "Available Time Slots",
        rows);
  }

  // -------------------------------------------------------------------------
  // 10. 15-MIN SLOTS
  // -------------------------------------------------------------------------

  public MetaInteractiveListMessage fifteenMinuteSlots(String yyyyMMdd, String slotRange) {

    String[] parts = slotRange.split("-");
    LocalTime start = LocalTime.of(Integer.parseInt(parts[0]), 0);
    LocalTime end = LocalTime.of(Integer.parseInt(parts[1]), 0);

    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");

    List<MetaListRow> rows = new ArrayList<>();

    LocalTime curr = start;

    while (curr.isBefore(end)) {
      LocalTime next = curr.plusMinutes(15);

      rows.add(
          row(
              "SLOT_%s_%s".formatted(yyyyMMdd, curr),
              "%s - %s".formatted(fmt.format(curr), fmt.format(next)),
              ""));

      curr = next;
    }

    return listMessage(
        "Select Preferred Slot",
        "Choose your appointment time:",
        "Select slot",
        "Select Slot",
        "Available 15-Min Slots",
        rows);
  }

  // -------------------------------------------------------------------------
  // 11. DATE LIST (RANGE)
  // -------------------------------------------------------------------------

  public MetaInteractiveListMessage expandedDateList(
      String start, String end, List<String> nextAvailableDates) {

    LocalDate s = LocalDate.parse(start);
    LocalDate e = LocalDate.parse(end);

    DateTimeFormatter label = DateTimeFormatter.ofPattern("dd MMM (EEE)");
    DateTimeFormatter payload = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Convert nextAvailableDates (List<String>) to a Set<LocalDate> for fast lookup
    Set<LocalDate> availableDates =
        nextAvailableDates.stream().map(LocalDate::parse).collect(Collectors.toSet());

    List<MetaListRow> rows = new ArrayList<>();
    LocalDate curr = s;

    while (!curr.isAfter(e)) {
      if (availableDates.contains(curr)) {
        rows.add(row("DATE_" + payload.format(curr), label.format(curr), ""));
      }
      curr = curr.plusDays(1);
    }

    return listMessage(
        "Available Dates",
        "Please select your preferred date:",
        "Select Date",
        "Select Date",
        "Available Dates",
        rows);
  }

  // -------------------------------------------------------------------------
  // 12. DATE LIST (less than 10)
  // -------------------------------------------------------------------------

  public MetaInteractiveListMessage expandedDateListWhenLesserThen10(List<String> dates) {

    DateTimeFormatter input = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter label = DateTimeFormatter.ofPattern("dd MMM (EEE)");
    DateTimeFormatter payload = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    List<MetaListRow> rows =
        dates.stream()
            .map(
                d -> {
                  LocalDate date = LocalDate.parse(d, input);
                  return row("DATE_" + payload.format(date), label.format(date), "");
                })
            .toList();

    return listMessage(
        "Available Dates",
        "Please select your preferred date:",
        "Select Date",
        "Select Date",
        "Available Dates",
        rows);
  }

  // -------------------------------------------------------------------------
  // 13. SHOW BOOKED APPOINTMENTS
  // -------------------------------------------------------------------------

  private MetaInteractiveListMessage buildBookedAppointmentMessage(
      List<Appointment> appointments, String title, String description) {

    List<MetaListRow> rows =
        appointments.stream()
            .map(
                apt -> {
                  Patient patient = patientRepository.findByPatientId(apt.getPatientId());
                  // Truncate name to fit within 24 char limit (Name DD-MM HH:MM format)
                  String name =
                      patient.getName().length() > 9
                          ? patient.getName().substring(0, 9)
                          : patient.getName();
                  String date =
                      apt.getDate().substring(6)
                          + "-"
                          + apt.getDate().substring(4, 6); // DD-MM from YYYYMMDD
                  String time = apt.getTime(); // HH:mm
                  String displayText = "%s %s %s".formatted(name, date, time);
                  return row(apt.getBookingId(), displayText, "");
                })
            .toList();

    return listMessage(
        title, description, "Select Appointment", "Select Booking", "Select Booking", rows);
  }

  public MetaInteractiveListMessage showTheBookedAppointment(List<Appointment> appointments) {
    return buildBookedAppointmentMessage(
        appointments,
        "Confirmed Appointments",
        "Please choose the appointment you want to cancel:");
  }

  public MetaInteractiveListMessage showTheBookedAppointmentToBeRescheduled(
      List<Appointment> appointments) {
    return buildBookedAppointmentMessage(
        appointments,
        "Confirmed Appointments",
        "Please choose the appointment you want to reschedule:");
  }

  // -------------------------------------------------------------------------
  // 14. WHATSAPP FLOWS
  // -------------------------------------------------------------------------

  /**
   * Build patient registration flow message
   *
   * @param flowId WhatsApp Flow ID for patient registration form
   * @return MetaFlowMessage ready to send
   */
  public MetaFlowMessage patientRegistrationFlow(String flowId) {
    return MetaFlowMessage.builder()
        .header("New Patient Registration")
        .body(
            "Please fill in your details to create your patient profile. This information helps us provide you with better healthcare services.")
        .footer("Your information is secure and confidential")
        .flowId(flowId)
        .flowCta("Register Now")
        .flowAction("navigate")
        .screenId("PATIENT_REG_SCREEN")
        .build();
  }

  /**
   * Build appointment booking flow message
   *
   * @param flowId WhatsApp Flow ID for appointment booking form
   * @param initialData Optional initial data to pre-fill the form
   * @return MetaFlowMessage ready to send
   */
  public MetaFlowMessage appointmentBookingFlow(String flowId, Map<String, Object> initialData) {
    return MetaFlowMessage.builder()
        .header("Book Appointment")
        .body("Select your preferred location, doctor, date and time for your appointment.")
        .footer("Available slots are shown in real-time")
        .flowId(flowId)
        .flowCta("Book Now")
        .flowAction("navigate")
        .screenId("APPOINTMENT_BOOKING_SCREEN")
        .flowActionPayload(initialData)
        .build();
  }

  /**
   * Build reschedule appointment flow message
   *
   * @param flowId WhatsApp Flow ID for reschedule form
   * @param appointmentId Appointment ID to reschedule
   * @return MetaFlowMessage ready to send
   */
  public MetaFlowMessage rescheduleAppointmentFlow(String flowId, String appointmentId) {
    Map<String, Object> initialData = Map.of("appointment_id", appointmentId);

    return MetaFlowMessage.builder()
        .header("Reschedule Appointment")
        .body("Select a new date and time for your appointment.")
        .footer("Your previous slot will be released")
        .flowId(flowId)
        .flowCta("Reschedule")
        .flowAction("navigate")
        .screenId("RESCHEDULE_SCREEN")
        .flowActionPayload(initialData)
        .build();
  }

  /**
   * Build cancel appointment flow message
   *
   * @param flowId WhatsApp Flow ID for cancellation form
   * @param appointmentId Appointment ID to cancel
   * @return MetaFlowMessage ready to send
   */
  public MetaFlowMessage cancelAppointmentFlow(String flowId, String appointmentId) {
    Map<String, Object> initialData = Map.of("appointment_id", appointmentId);

    return MetaFlowMessage.builder()
        .header("Cancel Appointment")
        .body("Please confirm your cancellation and optionally provide a reason.")
        .footer("Cancellation is effective immediately")
        .flowId(flowId)
        .flowCta("Cancel Appointment")
        .flowAction("navigate")
        .screenId("CANCEL_SCREEN")
        .flowActionPayload(initialData)
        .build();
  }

  /**
   * Build custom flow message
   *
   * @param flowId WhatsApp Flow ID
   * @param header Header text
   * @param body Body text
   * @param footer Footer text
   * @param cta Call-to-action button text
   * @param screenId Screen ID to navigate to
   * @return MetaFlowMessage ready to send
   */
  public MetaFlowMessage customFlow(
      String flowId, String header, String body, String footer, String cta, String screenId) {
    return MetaFlowMessage.builder()
        .header(header)
        .body(body)
        .footer(footer)
        .flowId(flowId)
        .flowCta(cta)
        .flowAction("navigate")
        .screenId(screenId)
        .build();
  }
}
