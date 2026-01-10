package com.divisha.model;

import com.divisha.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

  @Id private String id;

  @Email
  @Indexed(unique = true)
  private String email;

  @NotBlank private String name;

  private String password;

  @NotNull private Role role;

  /**
   * For dashboard, a user can have association(s) with specific location ids. Keep them as String
   * references to Location collection (not Location enum).
   */
  private List<String> locationIds;

  /**
   * If this user is a DOCTOR, link to the Doctor document (doctors collection). Only populated for
   * role == DOCTOR.
   */
  private String doctorId;

  @Indexed(unique = true, sparse = true)
  private String phone;
}
