package com.divisha.service;

import com.divisha.config.TimeZoneConfig;
import com.divisha.enums.AppointmentStatus;
import com.divisha.model.Appointment;
import com.divisha.model.Doctor;
import com.divisha.model.Location;
import com.divisha.model.MetaTemplateMessage;
import com.divisha.model.Patient;
import com.divisha.repository.AppointmentRepository;
import com.divisha.repository.DoctorRepository;
import com.divisha.repository.LocationRepository;
import com.divisha.repository.PatientRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentService {
  private final AppointmentRepository appointmentRepository;
  private final SlotService slotService;
  private final MetaMessageBuilder builder;
  private final MetaMessageSender sender;
  private final DoctorRepository doctorRepository;
  private final PatientRepository patientRepository;
  private final LocationRepository locationRepository;

  @Autowired private MongoTemplate mongoTemplate;

  @Value("${divisha.confirmation.fee:₹1200 (Payable at reception)}")
  private String feeText;

  @Value("${divisha.use.templates:false}")
  private boolean useTemplates;

  // @Todo Pankaj
  public List<Appointment> getApptByDate(String dateStr) {
    return new ArrayList<>();
  }

  public void createAppointment(Appointment appointment) {
    if (appointmentRepository.existsAppointmentByBookingId(appointment.getBookingId())) {
      appointment.setBookingId("BID-" + System.currentTimeMillis() / 1000L);
    }
    appointmentRepository.save(appointment);
  }

  public boolean isSlotAvailable(String date, String time, String doctor) {
    return !appointmentRepository.existsByDateAndTimeAndAndDoctorId(
        date.replace("-", ""), time, doctor);
  }

  public List<Appointment> getUpcomingAppointments(String phone) {
    String today =
        LocalDate.now(TimeZoneConfig.ZONE_ID).format(DateTimeFormatter.ISO_LOCAL_DATE); // yyyyMMdd
    return appointmentRepository.findByPhoneAndDateGreaterThanEqual(phone, today.replace("-", ""));
  }

  public List<Appointment> getUpcomingActiveAndWaitListedAppointments(String phone) {
    String today =
        LocalDate.now(TimeZoneConfig.ZONE_ID).format(DateTimeFormatter.ISO_LOCAL_DATE); // yyyyMMdd
    return appointmentRepository.findByPhoneAndStatusNotAndDateGreaterThanEqual(
        phone, AppointmentStatus.CANCELLED.name(), today.replace("-", ""));
  }

  public Appointment findByBookingId(String bookingId) {
    return appointmentRepository.findByBookingId(bookingId);
  }

  public Appointment updateAppointment(Appointment appointment) {
    return appointmentRepository.save(appointment);
  }

  public void updateAppointmentStatus(
      String rescheduledBookingId, String status, boolean sendAlert) {
    Appointment appointment = appointmentRepository.findByBookingId(rescheduledBookingId);
    appointment.setStatus(AppointmentStatus.CANCELLED.name());
    appointmentRepository.save(appointment);
    Optional<Doctor> doctorIdObj = doctorRepository.findById(appointment.getDoctorId());
    String doctorName = "";
    if (doctorIdObj.isPresent()) {
      Doctor doctor = doctorIdObj.get();
      doctorName = doctor.getName();
    }
    doctorName = doctorName.isEmpty() ? appointment.getDoctorId() : doctorName;

    if (sendAlert) {
      if (useTemplates) {
        // Send using WhatsApp template message
        sendCancellationTemplate(appointment, doctorName);
      } else {
        // Send using plain text message (existing behavior)
        sender.text(
            appointment.getPhone(),
            """
                        ❌Appointment Cancelled!*
                        Your appointment has been cancelled by the clinic.

                        🔖 *Booking ID:* %s
                        📅 *Date:* %s
                        🕒 *Time:* %s
                        🩺 *Doctor:* %s

                        We apologize for any inconvenience. Feel free to book a new appointment at your convenience.
                        Reply anytime to *book again* — just say Hi! 👋
                        """
                .formatted(
                    appointment.getBookingId(),
                    appointment.getDate(),
                    appointment.getTime(),
                    doctorName));
      }
    }
  }

  /**
   * Send appointment cancellation notification using WhatsApp template
   *
   * @param appointment The cancelled appointment
   * @param doctorName Doctor's name
   */
  private void sendCancellationTemplate(Appointment appointment, String doctorName) {
    // Format date as DD/MM/YYYY
    String formattedDate = formatDate(appointment.getDate());

    MetaTemplateMessage template =
        MetaTemplateMessage.builder()
            .templateName("appointment_cancelled_by_clinic")
            .languageCode("en")
            .build()
            .addBodyParameters(
                appointment.getBookingId(), formattedDate, appointment.getTime(), doctorName);

    if (appointment.getPhone() != null) {
      sender.sendTemplate(appointment.getPhone(), template);
    }
  }

  /**
   * Send appointment reschedule notification using WhatsApp template
   *
   * @param appointment The rescheduled appointment
   * @param doctorName Doctor's name
   */
  private void sendRescheduleTemplate(Appointment appointment, String doctorName) {
    // Format date as DD/MM/YYYY
    String formattedDate = formatDate(appointment.getDate());

    MetaTemplateMessage template =
        MetaTemplateMessage.builder()
            .templateName("appointment_rescheduled_by_clinic")
            .languageCode("en")
            .build()
            .addBodyParameters(
                appointment.getBookingId(),
                formattedDate,
                appointment.getTime(),
                doctorName,
                feeText);
    if (appointment.getPhone() != null) {
      sender.sendTemplate(appointment.getPhone(), template);
    }
  }

  /**
   * Helper method to format date from YYYYMMDD to DD/MM/YYYY
   *
   * @param yyyyMMdd Date in YYYYMMDD format
   * @return Date in DD/MM/YYYY format
   */
  private String formatDate(String yyyyMMdd) {
    if (yyyyMMdd == null || yyyyMMdd.length() != 8) {
      return yyyyMMdd;
    }
    try {
      LocalDate date = LocalDate.parse(yyyyMMdd, DateTimeFormatter.ofPattern("yyyyMMdd"));
      return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    } catch (Exception e) {
      return yyyyMMdd;
    }
  }

  public Appointment bookAppointment(Appointment appointment) {
    String bookingId = "BID-" + System.currentTimeMillis() / 1000L;
    appointment.setStatus(AppointmentStatus.CONFIRMED.toString());
    appointment.setBookingId(bookingId);
    appointment.setDate(appointment.getDate().replace("-", ""));
    Appointment savedAppointment = appointmentRepository.save(appointment);

    // Send booking confirmation
    sendBookingConfirmation(savedAppointment);

    return savedAppointment;
  }

  /**
   * Send appointment booking confirmation using WhatsApp template or plain text
   *
   * @param appointment The booked appointment
   */
  private void sendBookingConfirmation(Appointment appointment) {
    // Get patient name
    String patientName = "";
    Patient patient = patientRepository.findByPatientId(appointment.getPatientId());
    if (patient != null) {
      patientName = patient.getName();
    }

    // Get doctor name
    String doctorName = "";
    Optional<Doctor> doctorIdObj = doctorRepository.findById(appointment.getDoctorId());
    if (doctorIdObj.isPresent()) {
      doctorName = doctorIdObj.get().getName();
    }
    doctorName = doctorName.isEmpty() ? appointment.getDoctorId() : doctorName;

    // Get location name
    String locationName = "";
    Optional<Location> locationIdObj = locationRepository.findById(appointment.getLocationId());
    if (locationIdObj.isPresent()) {
      locationName = locationIdObj.get().getName();
    }
    locationName = locationName.isEmpty() ? appointment.getLocationId() : locationName;

    // Format date
    String formattedDate = formatDate(appointment.getDate());

    if (useTemplates) {
      // Send using WhatsApp template message
      sendBookingConfirmationTemplate(
          appointment, patientName, doctorName, locationName, formattedDate);
    } else {
      // Send using plain text message (existing behavior)
      sender.text(
          appointment.getPhone(),
          """
                    ✅ *Appointment Confirmed!*
                    Your appointment has been successfully booked.

                    👤 *Patient Name:* %s
                    🔖 *Booking ID:* %s
                    📅 *Date:* %s
                    🕒 *Time:* %s
                    🩺 *Doctor:* %s
                    📍 *Location:* %s
                    💰 *Consultation Fee:* %s

                    Please arrive 10 minutes early. Bring any previous medical records if available.

                    Thank you for choosing Divisha Arthritis & Medical Center 🙏
                    """
              .formatted(
                  patientName,
                  appointment.getBookingId(),
                  formattedDate,
                  appointment.getTime(),
                  doctorName,
                  locationName,
                  feeText));
    }
  }

  /**
   * Send appointment booking confirmation using WhatsApp template
   *
   * @param appointment The booked appointment
   * @param patientName Patient's name
   * @param doctorName Doctor's name
   * @param locationName Location name
   * @param formattedDate Formatted date
   */
  private void sendBookingConfirmationTemplate(
      Appointment appointment,
      String patientName,
      String doctorName,
      String locationName,
      String formattedDate) {

    MetaTemplateMessage template =
        MetaTemplateMessage.builder()
            .templateName("appointment_confirmed")
            .languageCode("en")
            .build()
            .addBodyParameters(
                patientName,
                appointment.getBookingId(),
                formattedDate,
                appointment.getTime(),
                doctorName,
                locationName,
                feeText);
    if (!appointment.getPhone().isEmpty()) {
      sender.sendTemplate(appointment.getPhone(), template);
    }
  }

  public List<Appointment> getAllAppointments(String date) {
    return appointmentRepository.findByDate(date).stream()
        .sorted(
            Comparator.comparing(
                Appointment::getTime, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  public Appointment rescheduleAppointment(Appointment appointment) {
    List<String> slots =
        slotService.getSlotsByDate(
            appointment.getDoctorId(), appointment.getDate().replace("-", ""), "");
    if (slots.contains(appointment.getTime())) {
      if (appointmentRepository.existsByDateAndTimeAndAndDoctorIdAndStatus(
          appointment.getDate().replace("-", ""),
          appointment.getTime(),
          appointment.getDoctorId(),
          AppointmentStatus.CONFIRMED.toString())) {
        throw new RuntimeException("Slot is already booked.");
      }
      appointment.setDate(appointment.getDate().replace("-", ""));
      appointment.setStatus(AppointmentStatus.CONFIRMED.toString());
      Appointment savedAppointment = appointmentRepository.save(appointment);

      Optional<Doctor> doctorIdObj = doctorRepository.findById(appointment.getDoctorId());
      String doctorName = "";
      if (doctorIdObj.isPresent()) {
        Doctor doctor = doctorIdObj.get();
        doctorName = doctor.getName();
      }
      doctorName = doctorName.isEmpty() ? appointment.getDoctorId() : doctorName;

      if (useTemplates) {
        // Send using WhatsApp template message
        sendRescheduleTemplate(savedAppointment, doctorName);
      } else {
        // Send using plain text message (existing behavior)
        sender.text(
            appointment.getPhone(),
            """
                        ✅ *Appointment Rescheduled!*
                            Your appointment has been rescheduled by the clinic.

                        🔖 *Booking ID:* %s
                        📅 *Date:* %s
                        🕒 *Time:* %s
                        🩺 *Doctor:* %s
                        💰 *Consultation Fee:* %s

                        If this time doesn't work for you, feel free to book a new slot.
                        See you soon! 👋
                        """
                .formatted(
                    savedAppointment.getBookingId(),
                    savedAppointment.getDate(),
                    savedAppointment.getTime(),
                    doctorName,
                    feeText));
      }

      return savedAppointment;
    }
    throw new RuntimeException("Slot not available");
  }

  public Appointment completeAppointment(String appointmentId) {
    Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
    if (appointment == null) {
      throw new RuntimeException("Appointment not found");
    }
    appointment.setStatus(AppointmentStatus.COMPLETED.toString());
    return appointmentRepository.save(appointment);
  }

  public Integer getTotalAppointments(String startDate, String endDate) {
    if (startDate == null || endDate == null) return 0;

    LocalDate start = LocalDate.parse(startDate);
    LocalDate end = LocalDate.parse(endDate);

    Query q = new Query();
    q.addCriteria(Criteria.where("date").gte(start.toString()).lte(end.toString()));

    long count = mongoTemplate.count(q, "appointment"); // collection name or Appointment.class
    return (int) count;
  }

  /** Variant: exclude CANCELLED appointments (common requirement) */
  public Integer getTotalNonCancelledAppointments(String startDate, String endDate) {
    if (startDate == null || endDate == null) return 0;

    LocalDate start = LocalDate.parse(startDate);
    LocalDate end = LocalDate.parse(endDate);

    Query q = new Query();
    q.addCriteria(
        Criteria.where("date")
            .gte(start.toString())
            .lte(end.toString())
            .and("status")
            .ne(AppointmentStatus.CANCELLED.name()));

    long count = mongoTemplate.count(q, "appointment");
    return (int) count;
  }
}
