package com.divisha.model;

import lombok.Data;

@Data
public class UpdatePasswordRequest {
  String username;
  String password;
  String newPassword;
  Boolean isAdmin;
}
