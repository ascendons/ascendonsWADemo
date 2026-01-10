package com.divisha.controller;

import com.divisha.model.Patient;
import com.divisha.model.RegisterPatient;
import com.divisha.service.PatientService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/patients")
public class PatientController {

  private final PatientService patientService;

  @GetMapping
  public ResponseEntity<List<Patient>> getAllPatients() {
    List<Patient> patients = patientService.getAllPatients();
    return ResponseEntity.ok(patients);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getPatientById(@PathVariable String id) {
    Optional<Patient> patient = patientService.getPatientById(id);
    if (patient.isPresent()) {
      return ResponseEntity.ok(patient.get());
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Patient not found with ID: " + id);
    }
  }

  @PostMapping
  public ResponseEntity<?> createPatient(@RequestBody RegisterPatient patient) {
    try {
      Patient created = patientService.createPatient(patient);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
    } catch (IllegalStateException e) {
      // e.g. if your service throws exception for validation
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error creating patient: " + e.getMessage());
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> updatePatient(
      @PathVariable String id, @RequestBody RegisterPatient patient) {
    try {
      Patient updated = patientService.updatePatient(id, patient);
      return ResponseEntity.ok(updated);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error updating patient: " + e.getMessage());
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deletePatient(@PathVariable String id) {
    try {
      patientService.deletePatient(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error deleting patient: " + e.getMessage());
    }
  }
}
