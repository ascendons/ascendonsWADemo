package com.divisha.model;

import com.divisha.enums.Role;
import java.util.List;
import lombok.Data;

@Data
public class UserResponse {
  private String id;
  private String name;
  private String email;
  private Role role;
  private List<String> locationIds;
  private String doctorId;
  private List<String> patientIds;
  private String phone;
}
