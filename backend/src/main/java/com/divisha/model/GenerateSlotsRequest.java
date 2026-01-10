package com.divisha.model;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSlotsRequest {
  @NotNull private LocalDate from;
  @NotNull private LocalDate to;
}
