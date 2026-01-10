package com.divisha.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

  @NotBlank private String username;

  @NotBlank private String password;

  public LoginRequest(String username, String password) {
    this.username = username;
    this.password = password;
  }
}
