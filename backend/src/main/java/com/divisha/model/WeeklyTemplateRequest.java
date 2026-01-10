package com.divisha.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTemplateRequest {
  @NotBlank private String doctorId;

  @Min(5)
  private int slotDurationMinutes = 30;

  private boolean active = true;

  @NotEmpty @Valid private List<TemplateRule> rules;

  private LocalDate effectiveFrom;
  private LocalDate effectiveTo;

  @AssertTrue(message = "effectiveFrom must be before or equal to effectiveTo")
  public boolean isEffectiveWindowValid() {
    if (effectiveFrom == null || effectiveTo == null) return true;
    return !effectiveFrom.isAfter(effectiveTo);
  }
}
