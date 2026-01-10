package com.divisha.controller;

import com.divisha.model.MetaTemplateMessage;
import com.divisha.service.MetaMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Test controller for WhatsApp templates */
@Slf4j
@RestController
@RequestMapping("/api/test-templates")
@RequiredArgsConstructor
public class TemplateTestController {

  private final MetaMessageSender sender;

  /**
   * Test appointment cancellation template
   *
   * <p>POST /api/test-templates/cancellation
   *
   * @param phone Phone number (without +, e.g., 918849434637)
   * @param bookingId Booking ID
   * @param date Date (DD/MM/YYYY)
   * @param time Time (HH:MM AM/PM)
   * @param doctor Doctor name
   * @return Success message
   */
  @PostMapping("/cancellation")
  public ResponseEntity<String> testCancellation(
      @RequestParam String phone,
      @RequestParam(defaultValue = "BID-TEST123") String bookingId,
      @RequestParam(defaultValue = "25/12/2025") String date,
      @RequestParam(defaultValue = "10:00 AM") String time,
      @RequestParam(defaultValue = "Dr. Test") String doctor) {

    log.info("🧪 Testing cancellation template - Phone: {}, BookingID: {}", phone, bookingId);

    MetaTemplateMessage template =
        MetaTemplateMessage.builder()
            .templateName("appointment_cancelled_by_clinic")
            .languageCode("en")
            .build()
            .addBodyParameters(bookingId, date, time, doctor);

    sender.sendTemplate(phone, template);

    return ResponseEntity.ok(
        String.format(
            "✅ Cancellation template sent to %s\nBooking ID: %s\nDate: %s\nTime: %s\nDoctor: %s",
            phone, bookingId, date, time, doctor));
  }

  /**
   * Test appointment reschedule template
   *
   * <p>POST /api/test-templates/reschedule
   */
  @PostMapping("/reschedule")
  public ResponseEntity<String> testReschedule(
      @RequestParam String phone,
      @RequestParam(defaultValue = "BID-TEST123") String bookingId,
      @RequestParam(defaultValue = "25/12/2025") String date,
      @RequestParam(defaultValue = "10:00 AM") String time,
      @RequestParam(defaultValue = "Dr. Test") String doctor,
      @RequestParam(defaultValue = "₹1200") String fee) {

    log.info("🧪 Testing reschedule template - Phone: {}, BookingID: {}", phone, bookingId);

    MetaTemplateMessage template =
        MetaTemplateMessage.builder()
            .templateName("appointment_rescheduled_by_clinic")
            .languageCode("en")
            .build()
            .addBodyParameters(bookingId, date, time, doctor, fee);

    sender.sendTemplate(phone, template);

    return ResponseEntity.ok(
        String.format(
            "✅ Reschedule template sent to %s\nBooking ID: %s\nDate: %s\nTime: %s\nDoctor: %s\nFee: %s",
            phone, bookingId, date, time, doctor, fee));
  }

  /**
   * Test appointment confirmed template
   *
   * <p>POST /api/test-templates/confirmed
   */
  @PostMapping("/confirmed")
  public ResponseEntity<String> testConfirmed(
      @RequestParam String phone,
      @RequestParam(defaultValue = "Test Patient") String patientName,
      @RequestParam(defaultValue = "BID-TEST123") String bookingId,
      @RequestParam(defaultValue = "25/12/2025") String date,
      @RequestParam(defaultValue = "10:00 AM") String time,
      @RequestParam(defaultValue = "Dr. Test") String doctor,
      @RequestParam(defaultValue = "Test Clinic") String location,
      @RequestParam(defaultValue = "₹1200") String fee) {

    log.info("🧪 Testing confirmed template - Phone: {}, BookingID: {}", phone, bookingId);

    MetaTemplateMessage template =
        MetaTemplateMessage.builder()
            .templateName("appointment_confirmed")
            .languageCode("en")
            .build()
            .addBodyParameters(patientName, bookingId, date, time, doctor, location, fee);

    sender.sendTemplate(phone, template);

    return ResponseEntity.ok(
        String.format(
            "✅ Confirmed template sent to %s\nPatient: %s\nBooking ID: %s\nDate: %s\nTime: %s\nDoctor: %s\nLocation: %s\nFee: %s",
            phone, patientName, bookingId, date, time, doctor, location, fee));
  }

  /**
   * Test hello_world template (pre-approved by WhatsApp)
   *
   * <p>POST /api/test-templates/hello-world
   */
  @PostMapping("/hello-world")
  public ResponseEntity<String> testHelloWorld(@RequestParam String phone) {

    log.info("🧪 Testing hello_world template - Phone: {}", phone);

    MetaTemplateMessage template =
        MetaTemplateMessage.builder().templateName("hello_world").languageCode("en_US").build();

    sender.sendTemplate(phone, template);

    return ResponseEntity.ok(String.format("✅ Hello World template sent to %s", phone));
  }

  /** Health check */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Template Test API is healthy ✅");
  }
}
