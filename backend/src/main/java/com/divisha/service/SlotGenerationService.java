package com.divisha.service;

import com.divisha.model.Slot;
import com.divisha.model.TemplateRule;
import com.divisha.model.WeeklyTemplate;
import com.divisha.repository.SlotRepository;
import com.divisha.repository.WeeklyTemplateRepository;
import java.time.*;
import java.time.temporal.IsoFields;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SlotGenerationService — updated to handle overlapping rules and priority: MONTHLY > BIWEEKLY >
 * WEEKLY
 */
@Service
@RequiredArgsConstructor
public class SlotGenerationService {

  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  private final WeeklyTemplateRepository templateRepo;
  private final SlotRepository slotRepo;
  private final MongoTemplate mongoTemplate;

  /**
   * Generate slots for templateId between startDate and endDate (inclusive). Returns number of
   * slots inserted.
   *
   * <p>Priority handling: - For a given date we collect matching rules, sort by priority (monthly,
   * biweekly, weekly) - We accept rules in that order; accepted rules "reserve" their time
   * intervals so lower-priority rules can't generate overlapping slots.
   *
   * <p>Atomic updates (biweekly/monthly) are only performed when the rule has passed the
   * time-conflict check.
   */
  @Transactional
  public int generateSlotsForTemplate(String templateId, LocalDate startDate, LocalDate endDate) {
    WeeklyTemplate template =
        templateRepo
            .findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

    if (!template.isActive()) return 0;

    int durationMinutes = template.getSlotDurationMinutes();

    List<Slot> toInsert = new ArrayList<>();

    // iterate each date in the inclusive interval
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      DayOfWeek dow = date.getDayOfWeek();

      // collect rules that apply to this day
      List<TemplateRule> dayRules = new ArrayList<>();
      for (TemplateRule rule : template.getRules()) {
        if (rule.getDayOfWeek() == dow) {
          dayRules.add(rule);
        }
      }

      if (dayRules.isEmpty()) continue;

      // sort rules by priority: MONTHLY (3) > BIWEEKLY (2) > WEEKLY (1)
      dayRules.sort(Comparator.comparingInt(this::recurrencePriority).reversed());

      // occupied intervals on this date (in local times) - accepted rules reserve these
      List<TimeInterval> occupied = new ArrayList<>();

      for (TemplateRule rule : dayRules) {
        // Defensive: ensure ruleId present
        if (rule.getRuleId() == null || rule.getRuleId().trim().isEmpty()) {
          throw new IllegalStateException("TemplateRule.ruleId is required for rule: " + rule);
        }

        LocalTime startLocal = rule.getStart();
        LocalTime endLocal = rule.getEnd();
        if (startLocal == null || endLocal == null) continue;
        if (!startLocal.isBefore(endLocal)) continue;

        // First quick checks (month occurrence for monthly). Do this BEFORE atomic operations.
        if (rule.getRecurrence() == TemplateRule.Recurrence.MONTHLY) {
          TemplateRule.MonthOccurrence mo = rule.getMonthOccurrence();
          if (mo != null && !monthOccurrenceMatches(date, mo)) {
            continue; // not the right occurrence within the month
          }
        }

        // Check if this rule's time range conflicts with any already-accepted rule for this date.
        if (conflictsWithOccupied(startLocal, endLocal, occupied)) {
          // conflict -> skip this rule and DO NOT touch atomic counters.
          continue;
        }

        // No conflict -> now perform atomic checks/marks for BIWEEKLY / MONTHLY
        boolean includeThisDate = false;
        switch (rule.getRecurrence()) {
          case WEEKLY:
            includeThisDate = true;
            break;
          case BIWEEKLY:
            // Atomically increment & get old counter (only now that we know no time conflict)
            int oldCount = atomicIncrementRuleCounterAndGetOld(templateId, rule.getRuleId());
            includeThisDate = (oldCount % 2 == 0);
            break;
          case MONTHLY:
            String monthKey = String.format("%04d-%02d", date.getYear(), date.getMonthValue());
            includeThisDate = atomicCheckAndMarkMonthly(templateId, rule.getRuleId(), monthKey);
            break;
          default:
            includeThisDate = true;
        }

        if (!includeThisDate) {
          // for BIWEEKLY this may have incremented the counter (expected behavior per atomic
          // design)
          // for MONTHLY atomicCheckAndMarkMonthly returned false -> already generated earlier
          continue;
        }

        // At this point, the rule is accepted for this date. Generate slot intervals (and mark them
        // occupied)
        LocalTime cursor = startLocal;
        List<TimeInterval> newlyReserved = new ArrayList<>();

        while (!cursor.plusMinutes(durationMinutes).isAfter(endLocal)) {
          LocalDateTime localStart = LocalDateTime.of(date, cursor);
          LocalDateTime localEnd = localStart.plusMinutes(durationMinutes);

          Instant utcStart = localStart.atZone(IST).toInstant();
          Instant utcEnd = localEnd.atZone(IST).toInstant();

          // deterministic id to make generation idempotent
          String slotId = deterministicSlotId(templateId, rule.getRuleId(), utcStart);

          // skip if slot already exists
          if (slotRepo.existsById(slotId)) {
            cursor = cursor.plusMinutes(durationMinutes);
            continue;
          }

          Slot s = new Slot();
          s.setId(slotId);
          s.setSourceTemplateId(templateId);
          s.setDoctorId(template.getDoctorId());
          s.setLocationId(rule.getLocationId());
          s.setStart(utcStart);
          s.setEnd(utcEnd);
          s.setAvailable(true);
          s.setLocalDate(date);
          s.setDayOfWeek(dow);

          Map<String, Object> meta = new HashMap<>();
          meta.put("createdBy", "template-gen");
          meta.put("ruleId", rule.getRuleId());
          meta.put("recurrence", rule.getRecurrence().name());
          meta.put("localStart", localStart.toString());
          meta.put("generatedForRangeStart", startDate.toString());
          meta.put("generatedForRangeEnd", endDate.toString());
          s.setMetadata(meta);

          toInsert.add(s);

          // reserve this slot's local-time interval so lower-priority rules won't overlap
          newlyReserved.add(new TimeInterval(cursor, cursor.plusMinutes(durationMinutes)));

          cursor = cursor.plusMinutes(durationMinutes);
        }

        // Merge newlyReserved intervals into occupied list
        occupied.addAll(newlyReserved);
      } // end rules loop for date
    } // end date loop

    if (toInsert.isEmpty()) return 0;

    List<Slot> saved = slotRepo.saveAll(toInsert);
    return saved.size();
  }

  // recurrence priority: higher number == higher priority
  private int recurrencePriority(TemplateRule rule) {
    if (rule.getRecurrence() == null) return 1;
    return switch (rule.getRecurrence()) {
      case MONTHLY -> 3;
      case BIWEEKLY -> 2;
      case WEEKLY -> 1;
    };
  }

  // Check if a time interval [start, end) conflicts with any occupied intervals
  private boolean conflictsWithOccupied(
      LocalTime start, LocalTime end, List<TimeInterval> occupied) {
    for (TimeInterval it : occupied) {
      if (intervalsOverlap(start, end, it.start, it.end)) return true;
    }
    return false;
  }

  // two half-open intervals [aStart,aEnd) and [bStart,bEnd) overlap if aStart < bEnd && bStart <
  // aEnd
  private boolean intervalsOverlap(
      LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
    return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
  }

  // deterministic slot id: templateId|ruleId|utcStartISO
  private String deterministicSlotId(String templateId, String ruleId, Instant utcStart) {
    return templateId + "|" + ruleId + "|" + utcStart.toString();
  }

  /**
   * Atomically increments rule counter and returns the previous value (old counter). Uses
   * weekly_templates.ruleCounters.<ruleId>
   *
   * <p>Note: This method mutates the stored counter (increment) and returns the previous number.
   * Caller should only invoke when it intends to consume/advance the biweekly counter.
   */
  private int atomicIncrementRuleCounterAndGetOld(String templateId, String ruleId) {
    String field = "ruleCounters." + ruleId;

    Query query = Query.query(Criteria.where("_id").is(templateId));
    Update update = new Update().inc(field, 1);

    // Return the *old* document (before increment). returnNew(false) means we want the pre-update
    // version.
    FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(false).upsert(true);

    WeeklyTemplate previous =
        mongoTemplate.findAndModify(
            query, update, options, WeeklyTemplate.class, "weekly_templates");

    if (previous == null) {
      // if previous document was null, counter was effectively 0 before increment
      return 0;
    }

    Map<String, Integer> counters = previous.getRuleCounters();
    if (counters == null) {
      return 0;
    }

    return counters.getOrDefault(ruleId, 0);
  }

  /**
   * For monthly rules: atomically set ruleLastGeneratedMonth.ruleId = monthKey if it's not already
   * equal. Returns true if the update matched and set (meaning this generator is the first to mark
   * this month) Returns false if the field was already equal to monthKey (slot already generated
   * for month).
   */
  private boolean atomicCheckAndMarkMonthly(String templateId, String ruleId, String monthKey) {
    String field = "ruleLastGeneratedMonth." + ruleId;

    Query q =
        Query.query(
            Criteria.where("_id")
                .is(templateId)
                .andOperator(
                    new Criteria()
                        .orOperator(
                            Criteria.where(field).exists(false),
                            Criteria.where(field).ne(monthKey))));

    Update u = new Update().set(field, monthKey);

    FindAndModifyOptions opts = FindAndModifyOptions.options().returnNew(false).upsert(true);

    WeeklyTemplate prev =
        mongoTemplate.findAndModify(q, u, opts, WeeklyTemplate.class, "weekly_templates");

    return prev != null;
  }

  private boolean monthOccurrenceMatches(LocalDate date, TemplateRule.MonthOccurrence occurrence) {
    int occ =
        switch (occurrence) {
          case FIRST -> 1;
          case SECOND -> 2;
          case THIRD -> 3;
          case FOURTH -> 4;
          case LAST -> -1;
          default -> 0;
        };
    if (occ == 0) return true;

    if (occ == -1) {
      LocalDate plus7 = date.plusWeeks(1);
      return plus7.getMonth() != date.getMonth();
    } else {
      int occurrenceOfWeek = ((date.getDayOfMonth() - 1) / 7) + 1;
      return occurrenceOfWeek == occ;
    }
  }

  // convenience helpers for iso week numbers, using IST
  private int isoWeekNumber(LocalDate date) {
    return date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
  }

  private int isoWeekNumber(Instant instant, ZoneId zone) {
    LocalDate ld = LocalDateTime.ofInstant(instant, zone).toLocalDate();
    return isoWeekNumber(ld);
  }

  // small helper class to model reserved time intervals on a single date (local times)
  private static class TimeInterval {
    final LocalTime start;
    final LocalTime end;

    TimeInterval(LocalTime s, LocalTime e) {
      this.start = s;
      this.end = e;
    }
  }
}
