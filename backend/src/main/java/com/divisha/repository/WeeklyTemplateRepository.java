package com.divisha.repository;

import com.divisha.model.WeeklyTemplate;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WeeklyTemplateRepository extends MongoRepository<WeeklyTemplate, String> {
  List<WeeklyTemplate> findByDoctorId(String doctorId);
}
