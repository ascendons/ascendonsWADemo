package com.divisha.service;

import com.divisha.enums.Role;
import com.divisha.enums.Status;
import com.divisha.model.*;
import com.divisha.repository.DoctorRepository;
import com.divisha.repository.DoctorScheduleRepository;
import com.divisha.repository.PatientRepository;
import com.divisha.repository.UserRepository;
import com.divisha.util.JwtTokenUtil;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final PatientRepository patientRepository;
  private final DoctorRepository doctorRepository;
  private final DoctorScheduleRepository doctorScheduleRepository;
  private JwtTokenUtil jwtTokenUtil;

  public AuthService(
      UserRepository userRepository,
      JwtTokenUtil jwtTokenUtil,
      DoctorRepository doctorRepository,
      PasswordEncoder passwordEncoder,
      DoctorScheduleRepository doctorScheduleRepository,
      PatientRepository patientRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.patientRepository = patientRepository;
    this.doctorRepository = doctorRepository;
    this.doctorScheduleRepository = doctorScheduleRepository;
    this.jwtTokenUtil = jwtTokenUtil;
  }

  /** Authenticate user by email and password. Uses DB as single source of truth. */
  public LoginResponse login(String username, String password) throws AuthenticationException {
    Optional<User> userOpt = userRepository.findByEmail(username);
    if (userOpt.isEmpty()) {
      throw new AuthenticationException("Invalid credentials");
    }

    User user = userOpt.get();
    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new AuthenticationException("Invalid credentials");
    }

    String token = jwtTokenUtil.generateToken(user.getId(), user.getRole().toString());

    return new LoginResponse(token, "Login successful", user.getId());
  }

  public void updatePassword(UpdatePasswordRequest request) throws AuthenticationException {
    Optional<User> userOpt = userRepository.findByEmail(request.getUsername());
    if (userOpt.isEmpty()) {
      throw new AuthenticationException("User not found");
    }
    User user = userOpt.get();
    if (!request.getIsAdmin()
        && !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new AuthenticationException("Invalid current password");
    }
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
  }

  /** Register a new user. Encodes password and persists user to DB. */
  @Transactional
  public RegisterResponse registerUser(RegisterRequest req) throws UserAlreadyExistsException {
    if (userRepository.existsByEmail(req.getEmail())) {
      throw new UserAlreadyExistsException("User already exists");
    }
    System.out.println("Registering user: " + req);
    User entity = new User();
    entity.setEmail(req.getEmail());
    entity.setName(req.getName());
    entity.setPassword(passwordEncoder.encode(req.getPassword()));
    entity.setRole(req.getRole());
    entity.setLocationIds(req.getLocationIds());
    userRepository.save(entity);
    if (req.getRole().equals(Role.DOCTOR)) {
      Doctor doctor = new Doctor();
      doctor.setId(entity.getId());
      doctor.setName("Dr. " + entity.getName());
      doctor.setStatus(Status.ACTIVE.name());
      doctor.setCreatedAt(System.currentTimeMillis() / 1000L);
      doctor.setSpecialization(req.getSpecialization());
      doctor.setGender(req.getGender());
      doctorRepository.save(doctor);
      entity.setDoctorId(doctor.getId());
      userRepository.save(entity);
      DoctorSchedule doctorSchedule = new DoctorSchedule();
      doctorSchedule.setDoctorId(entity.getId());
      doctorSchedule.setDoctorName(entity.getName());
      doctorSchedule.setStartTime("09:00");
      doctorSchedule.setEndTime("18:00");
      doctorSchedule.setUnavailableDaysOfWeek(List.of("SUNDAY"));
      doctorScheduleRepository.save(doctorSchedule);
    }
    System.out.println("User registered successfully: " + entity);
    return new RegisterResponse("User registered successfully");
  }

  @Transactional
  public RegisterResponse registerNewPatient(RegisterPatient req)
      throws UserAlreadyExistsException {
    List<Patient> patients = patientRepository.findByPhone(req.getPhone());
    if (patients.size() > 2) {
      throw new UserAlreadyExistsException(
          "There are 3 patients already registered with this number");
    } else {
      Patient patient = new Patient();
      patient.setPhone(req.getPhone());
      patient.setName(req.getName());
      patient.setGender(req.getGender());
      patient.setAge(req.getAge());
      patient.setNotes(req.getNotes());
      patient.setCreatedAt(Instant.now());
      patientRepository.save(patient);
      return new RegisterResponse("Patient registered successfully");
    }
  }

  public UserResponse getUser(String id) {
    User user = userRepository.findById(id).orElse(null);
    if (user == null) {
      throw new RuntimeException("User not found");
    }
    return convertToUserResponse(user);
  }

  public List<UserResponse> getAllUsers() {
    return userRepository.findAll().stream()
        .map(this::convertToUserResponse)
        .collect(Collectors.toList());
  }

  public List<UserResponse> getUsersByRole(String role) {
    return userRepository.findAll().stream()
        .filter(user -> user.getRole().toString().equals(role))
        .map(this::convertToUserResponse)
        .collect(Collectors.toList());
  }

  private UserResponse convertToUserResponse(User user) {
    UserResponse userResponse = new UserResponse();
    userResponse.setId(user.getId());
    userResponse.setName(user.getName());
    userResponse.setEmail(user.getEmail());
    userResponse.setRole(user.getRole());
    userResponse.setLocationIds(user.getLocationIds());
    userResponse.setDoctorId(user.getDoctorId());
    userResponse.setPhone(user.getPhone());
    return userResponse;
  }

  public StatusResponse deleteUser(String id) {
    Optional<User> user = userRepository.findById(id);
    if (user.isEmpty()) {
      throw new RuntimeException("User not found");
    }
    if (user.get().getRole() == Role.DOCTOR) {
      doctorRepository.deleteById(user.get().getDoctorId());
      doctorScheduleRepository.deleteByDoctorId(user.get().getDoctorId());
    }
    userRepository.delete(user.get());
    return new StatusResponse("User deleted successfully");
  }

  public static class AuthenticationException extends Exception {
    public AuthenticationException(String msg) {
      super(msg);
    }
  }

  public static class UserAlreadyExistsException extends Exception {
    public UserAlreadyExistsException(String msg) {
      super(msg);
    }
  }
}
