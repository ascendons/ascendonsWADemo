package com.divisha.repository;

import com.divisha.model.Slot;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SlotRepository extends MongoRepository<Slot, String> {
  List<Slot> findByDoctorIdAndLocationId(String doctorId, String locationId);

  List<Slot> findByDoctorIdAndLocationIdAndStartAfter(
      String doctorId, String locationId, Instant after);

  boolean existsByDoctorIdAndLocationIdAndStart(String doctorId, String locationId, Instant start);

  long countBySourceTemplateIdAndStartBetween(String sourceTemplateId, Instant start, Instant end);

  List<Slot> findByLocationIdAndStartAfter(String locationId, Instant after);

  List<Slot> findByDoctorIdAndStartAfter(String doctorId, Instant after);

  List<Slot> findByStartAfter(Instant after);

  List<Slot> findByLocationId(String locationId);
}
