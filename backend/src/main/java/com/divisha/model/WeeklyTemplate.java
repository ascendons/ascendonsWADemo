package com.divisha.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Recurring template for a doctor. - Contains weekly, biweekly, and monthly recurrence rules. -
 * Tracks counters for deterministic biweekly/monthly generation. - Use versioning/updatedAt for
 * safe regeneration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "weekly_templates")
public class WeeklyTemplate {

  @Id private String id;

  @NotBlank private String doctorId;

  @Min(1)
  private int slotDurationMinutes = 15;

  private boolean active = true;

  @NotEmpty private List<TemplateRule> rules;

  /**
   * Counter map for each rule to track biweekly/recurring generation state. key = ruleId, value =
   * number of times the rule has been considered/generated.
   */
  private Map<String, Integer> ruleCounters = new HashMap<>();

  /**
   * For monthly rules, stores the last generated month (YYYY-MM) for each rule. Used to ensure
   * "once per month" rules don't generate multiple times.
   */
  private Map<String, String> ruleLastGeneratedMonth = new HashMap<>();

  @CreatedDate private Instant createdAt = Instant.now();

  @LastModifiedDate private Instant updatedAt = Instant.now();

  @Version private Long version;
}
