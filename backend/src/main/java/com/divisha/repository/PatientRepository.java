package com.divisha.repository;

import com.divisha.model.Patient;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PatientRepository extends MongoRepository<Patient, String> {
  List<Patient> findByPhone(String phone);

  Patient findByPatientId(String patientId);

  @Query(
      value = "{ 'name': { $regex: ?0, $options: 'i' } }",
      fields = "{ '_id': 1, 'name': 1, 'phone': 1 }")
  List<Patient> findByNameRegex(String regex);
}
