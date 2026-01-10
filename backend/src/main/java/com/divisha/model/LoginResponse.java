package com.divisha.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {

  private String token;
  private String message;
  private String userId;

  public LoginResponse(String token, String message, String userId) {
    this.token = token;
    this.message = message;
    this.userId = userId;
  }
}
