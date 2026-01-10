package com.divisha.repository;

import com.divisha.model.DoctorSchedule;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DoctorScheduleRepository extends MongoRepository<DoctorSchedule, String> {

  // All schedules for a doctor (multi-location support)
  List<DoctorSchedule> findByDoctorId(String doctorId);

  // All schedules for a location (multi-doctor support)
  List<DoctorSchedule> findByLocationId(String locationId);

  // Used when doctor is tied to specific location
  List<DoctorSchedule> findByDoctorIdAndLocationId(String doctorId, String locationId);

  DoctorSchedule findByDoctorName(String doctorName);

  void deleteByDoctorId(String doctorId);

  //  DoctorSchedule findByDoctorId(String doctorId);
}
