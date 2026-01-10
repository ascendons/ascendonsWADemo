package com.divisha.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Materialized, bookable slot instance. - start/end stored as Instant (UTC). - localDate stored to
 * simplify date-based queries in doctor's timezone. - metadata: flexible map for provenance and
 * flags.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "slots")
@CompoundIndexes({
  @CompoundIndex(name = "doctor_start_unique", def = "{'doctorId': 1, 'start': 1}", unique = true),
  @CompoundIndex(
      name = "doctor_localDate_available",
      def = "{'doctorId': 1, 'localDate': 1, 'isAvailable': 1}")
})
public class Slot {
  @Id private String id;

  @Indexed private String doctorId;

  /** Reference to Location.id (string). Use location doc for timezone/address. */
  private String locationId;

  private Instant start;
  private Instant end;

  /**
   * Derived local date computed during generation using the Location.timezone. Helpful for UI daily
   * queries and grouping.
   */
  private LocalDate localDate;

  private DayOfWeek dayOfWeek;

  private boolean isAvailable = true;

  /** Source template that generated this slot (nullable for ad-hoc slots) */
  private String sourceTemplateId;

  @CreatedDate private Instant generatedAt = Instant.now();

  /** Flexible attachment for provenance, external ids, manual override info, etc. */
  private Map<String, Object> metadata;

  /** optimistic locking for safe concurrent updates */
  @Version private Long version;
}
