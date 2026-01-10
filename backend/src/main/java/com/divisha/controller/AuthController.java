package com.divisha.controller;

import com.divisha.model.*;
import com.divisha.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Simple login endpoint. Accepts username & password and returns a login response (token or
   * message).
   */
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    try {
      LoginResponse resp = authService.login(request.getUsername(), request.getPassword());
      // include Authorization header so frontend can read it (we'll expose this header in CORS)
      return ResponseEntity.ok()
          .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer " + resp.getToken())
          .body(resp);
    } catch (AuthService.AuthenticationException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new LoginResponse(null, "Invalid credentials", null));
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new LoginResponse(null, "Internal error", null));
    }
  }

  @PutMapping("/updatePassword")
  public ResponseEntity<String> updatePassword(@RequestBody UpdatePasswordRequest request) {
    try {
      authService.updatePassword(request);
      return ResponseEntity.ok("Password updated successfully");
    } catch (AuthService.AuthenticationException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }
  }

  @GetMapping("/user")
  public ResponseEntity<UserResponse> getUser(@RequestParam String id) {
    return ResponseEntity.status(HttpStatus.OK).body(authService.getUser(id));
  }

  @DeleteMapping("/user")
  public ResponseEntity<StatusResponse> deleteUser(@RequestParam String id) {
    return ResponseEntity.status(HttpStatus.OK).body(authService.deleteUser(id));
  }

  @GetMapping("/allUsers")
  public ResponseEntity<List<UserResponse>> getAllUsers() {
    return ResponseEntity.status(HttpStatus.OK).body(authService.getAllUsers());
  }

  @GetMapping("usersByRole")
  public ResponseEntity<List<UserResponse>> getUsersByRole(@RequestParam String role) {
    return ResponseEntity.status(HttpStatus.OK).body(authService.getUsersByRole(role));
  }

  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    System.out.println("Registering user: " + request);
    try {
      System.out.println("Registering user: " + request);
      RegisterResponse resp = authService.registerUser(request);
      System.out.println("User registered successfully: " + resp);
      return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    } catch (AuthService.UserAlreadyExistsException ex) {

      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(new RegisterResponse("User already exists"));
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new RegisterResponse("Internal error"));
    }
  }

  @PostMapping("/register-patient")
  public ResponseEntity<RegisterResponse> registerPatient(
      @Valid @RequestBody RegisterPatient request) {
    try {
      RegisterResponse resp = authService.registerNewPatient(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    } catch (AuthService.UserAlreadyExistsException ex) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(new RegisterResponse("User already exists"));
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new RegisterResponse("Internal error"));
    }
  }

  /** Optional health-check endpoint for auth. */
  @GetMapping("/ping")
  public ResponseEntity<String> ping() {
    return ResponseEntity.ok("auth-service-ok");
  }
}
