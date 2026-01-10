package com.divisha.service;

import com.divisha.model.Patient;
import com.divisha.model.RegisterPatient;
import com.divisha.repository.PatientRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientService {
  private static final AtomicInteger atomicInteger = new AtomicInteger();

  private final PatientRepository patientRepository;

  public List<Patient> getAllPatients() {
    return patientRepository.findAll();
  }

  public Optional<Patient> getPatientById(String id) {
    return patientRepository.findById(id);
  }

  public Patient createPatient(RegisterPatient request) {

    String phone = request.getPhone();
    String name = request.getName() != null ? request.getName().trim() : null;

    if (phone == null || phone.isBlank()) {
      throw new IllegalArgumentException("Phone number is required");
    }

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Patient name is required");
    }
    String patientId = "DV-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    request.setPatientId(patientId);

    List<Patient> existingPatients = patientRepository.findByPhone(phone);
    if (existingPatients.size() >= 3) {
      throw new IllegalStateException(
          "This phone number already has 3 patients registered. Please try another number.");
    }
    boolean nameExists =
        existingPatients.stream().anyMatch(p -> p.getName().equalsIgnoreCase(name));

    if (nameExists) {
      throw new IllegalStateException(
          "A patient with this name already exists for this phone number. Please use a different name.");
    }
    Patient entity = dtoToEntity(request);
    return patientRepository.save(entity);
  }

  public List<Patient> getPatientByPhone(String phone) {
    return patientRepository.findByPhone(phone);
  }

  public Patient updatePatient(String id, RegisterPatient patient) {

    Optional<Patient> optionalPatient = patientRepository.findById(id);
    if (optionalPatient.isEmpty()) {
      throw new RuntimeException("Patient not found");
    }
    Patient updatedPatient = optionalPatient.get();
    updatedPatient.setName(patient.getName());
    updatedPatient.setGender(patient.getGender());
    updatedPatient.setAge(patient.getAge());
    updatedPatient.setNotes(patient.getNotes());
    return patientRepository.save(updatedPatient);
  }

  public void deletePatient(String id) {
    patientRepository.deleteById(id);
  }

  private Patient dtoToEntity(RegisterPatient dto) {
    Patient entity = new Patient();
    entity.setPhone(dto.getPhone());
    entity.setName(dto.getName());
    entity.setGender(dto.getGender());
    entity.setAge(dto.getAge());
    entity.setNotes(dto.getNotes());
    entity.setPatientId(dto.getPatientId());
    entity.setCreatedAt(Instant.now());
    return entity;
  }
}
