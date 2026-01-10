package com.divisha.flow.config;

import com.divisha.enums.Gender;
import com.divisha.model.Doctor;
import com.divisha.model.DoctorSchedule;
import com.divisha.model.Location;
import com.divisha.repository.DoctorRepository;
import com.divisha.repository.DoctorScheduleRepository;
import com.divisha.repository.LocationRepository;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Initializes sample data for development and testing. This will populate the database with sample
 * doctors, locations, and schedules if they don't exist.
 */
@Slf4j
@Configuration
@Profile("!prod") // Only run in non-production environments
@RequiredArgsConstructor
public class SampleDataInitializer {

  private final DoctorRepository doctorRepository;
  private final LocationRepository locationRepository;
  private final DoctorScheduleRepository doctorScheduleRepository;

  @Bean
  public CommandLineRunner initSampleData() {
    return args -> {
      log.info("🔧 Initializing sample data for WhatsApp Flow appointment system...");

      // Check if data already exists
      if (doctorRepository.count() > 0) {
        log.info("✓ Sample data already exists, skipping initialization");
        return;
      }

      // Create sample locations
      Location location1 =
          new Location("LOC001", "Downtown Medical Center", "123 Main Street, Suite 100", "active");
      Location location2 =
          new Location("LOC002", "Northside Clinic", "456 Oak Avenue, Building B", "active");
      Location location3 =
          new Location("LOC003", "Westside Hospital", "789 Elm Road, Floor 3", "active");

      locationRepository.saveAll(Arrays.asList(location1, location2, location3));
      log.info("✓ Created {} locations", 3);

      // Create sample doctors
      Doctor doctor1 =
          new Doctor(
              "DOC001",
              "Dr. Sarah Johnson",
              "Cardiologist",
              Gender.FEMALE,
              "active",
              System.currentTimeMillis());

      Doctor doctor2 =
          new Doctor(
              "DOC002",
              "Dr. Michael Chen",
              "Pediatrician",
              Gender.MALE,
              "active",
              System.currentTimeMillis());

      Doctor doctor3 =
          new Doctor(
              "DOC003",
              "Dr. Emily Rodriguez",
              "Dermatologist",
              Gender.FEMALE,
              "active",
              System.currentTimeMillis());

      Doctor doctor4 =
          new Doctor(
              "DOC004",
              "Dr. James Williams",
              "Orthopedic Surgeon",
              Gender.MALE,
              "active",
              System.currentTimeMillis());

      doctorRepository.saveAll(Arrays.asList(doctor1, doctor2, doctor3, doctor4));
      log.info("✓ Created {} doctors", 4);

      // Create doctor schedules
      // Dr. Sarah Johnson - Downtown Medical Center (Mon-Fri, 9 AM - 5 PM)
      DoctorSchedule schedule1 =
          DoctorSchedule.builder()
              .doctorId("DOC001")
              .doctorName("Dr. Sarah Johnson")
              .locationId("LOC001")
              .startTime("09:00")
              .endTime("17:00")
              .unavailableDaysOfWeek(Arrays.asList("SATURDAY", "SUNDAY"))
              .unavailableDates(List.of())
              .customDateSlots(List.of())
              .build();

      // Dr. Sarah Johnson - Westside Hospital (Mon, Wed, Fri, 2 PM - 8 PM)
      DoctorSchedule schedule2 =
          DoctorSchedule.builder()
              .doctorId("DOC001")
              .doctorName("Dr. Sarah Johnson")
              .locationId("LOC003")
              .startTime("14:00")
              .endTime("20:00")
              .unavailableDaysOfWeek(Arrays.asList("TUESDAY", "THURSDAY", "SATURDAY", "SUNDAY"))
              .unavailableDates(List.of())
              .customDateSlots(List.of())
              .build();

      // Dr. Michael Chen - Northside Clinic (Mon-Sat, 8 AM - 4 PM)
      DoctorSchedule schedule3 =
          DoctorSchedule.builder()
              .doctorId("DOC002")
              .doctorName("Dr. Michael Chen")
              .locationId("LOC002")
              .startTime("08:00")
              .endTime("16:00")
              .unavailableDaysOfWeek(List.of("SUNDAY"))
              .unavailableDates(List.of())
              .customDateSlots(List.of())
              .build();

      // Dr. Emily Rodriguez - Downtown Medical Center (Tue-Sat, 10 AM - 6 PM)
      DoctorSchedule schedule4 =
          DoctorSchedule.builder()
              .doctorId("DOC003")
              .doctorName("Dr. Emily Rodriguez")
              .locationId("LOC001")
              .startTime("10:00")
              .endTime("18:00")
              .unavailableDaysOfWeek(Arrays.asList("SUNDAY", "MONDAY"))
              .unavailableDates(List.of())
              .customDateSlots(List.of())
              .build();

      // Dr. Emily Rodriguez - Westside Hospital (Mon-Fri, 9 AM - 3 PM)
      DoctorSchedule schedule5 =
          DoctorSchedule.builder()
              .doctorId("DOC003")
              .doctorName("Dr. Emily Rodriguez")
              .locationId("LOC003")
              .startTime("09:00")
              .endTime("15:00")
              .unavailableDaysOfWeek(Arrays.asList("SATURDAY", "SUNDAY"))
              .unavailableDates(List.of())
              .customDateSlots(List.of())
              .build();

      // Dr. James Williams - Northside Clinic (Mon-Thu, 9 AM - 5 PM)
      DoctorSchedule schedule6 =
          DoctorSchedule.builder()
              .doctorId("DOC004")
              .doctorName("Dr. James Williams")
              .locationId("LOC002")
              .startTime("09:00")
              .endTime("17:00")
              .unavailableDaysOfWeek(Arrays.asList("FRIDAY", "SATURDAY", "SUNDAY"))
              .unavailableDates(List.of())
              .customDateSlots(List.of())
              .build();

      // Dr. James Williams - Downtown Medical Center (Fri, 8 AM - 2 PM)
      DoctorSchedule schedule7 =
          DoctorSchedule.builder()
              .doctorId("DOC004")
              .doctorName("Dr. James Williams")
              .locationId("LOC001")
              .startTime("08:00")
              .endTime("14:00")
              .unavailableDaysOfWeek(
                  Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "SATURDAY", "SUNDAY"))
              .unavailableDates(List.of())
              .customDateSlots(List.of())
              .build();

      doctorScheduleRepository.saveAll(
          Arrays.asList(
              schedule1, schedule2, schedule3, schedule4, schedule5, schedule6, schedule7));
      log.info("✓ Created {} doctor schedules", 7);

      log.info(
          "✅ Sample data initialization complete! The system now has {} doctors across {} locations",
          doctorRepository.count(),
          locationRepository.count());
    };
  }
}
