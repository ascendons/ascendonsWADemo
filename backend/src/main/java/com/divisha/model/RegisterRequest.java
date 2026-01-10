package com.divisha.model;

import com.divisha.enums.Gender;
import com.divisha.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

  @Email @NotBlank private String email;

  @NotBlank private String name;

  @NotBlank private String password;

  @NotNull private Role role;

  private List<String> locationIds;

  private String specialization;

  private Gender gender;

  public RegisterRequest(
      String email, String name, String password, Role role, List<String> locationIds) {
    this.email = email;
    this.name = name;
    this.password = password;
    this.role = role;
    this.locationIds = locationIds;
  }
}
