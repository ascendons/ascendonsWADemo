package com.divisha.repository;

import com.divisha.model.Appointment;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppointmentRepository extends MongoRepository<Appointment, String> {
  boolean existsByDateAndTimeAndAndDoctorId(String date, String time, String doctor);

  boolean existsAppointmentByBookingId(String bookingId);

  List<Appointment> findByPhoneAndDateGreaterThanEqual(String phone, String date);

  Appointment findByBookingId(String bookingId);

  List<Appointment> findByDate(String date);

  boolean existsByDateAndTimeAndAndDoctorIdAndStatus(
      String date, String time, String doctor, String status);

  long countByDateBetween(String startDate, String endDate);

  // or exclude cancelled:
  long countByDateBetweenAndStatusNot(String startDate, String endDate, String statusToExclude);

  List<Appointment> findByPhoneAndStatusAndDateGreaterThanEqual(
      String phone, String status, String date);

  List<Appointment> findByPhoneAndStatusNotAndDateGreaterThanEqual(
      String phone, String statusToExclude, String date);

  List<Appointment> findByDoctorIdAndLocationIdAndDate(
      String doctorId, String locationId, String date);
}
