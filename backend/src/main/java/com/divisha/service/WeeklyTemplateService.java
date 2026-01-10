package com.divisha.service;

import com.divisha.model.Slot;
import com.divisha.model.TemplateRule;
import com.divisha.model.WeeklyTemplate;
import com.divisha.model.WeeklyTemplateRequest;
import com.divisha.repository.SlotRepository;
import com.divisha.repository.WeeklyTemplateRepository;
import jakarta.annotation.PostConstruct;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyTemplateService {

  private final WeeklyTemplateRepository templateRepo;
  private final SlotRepository slotRepo;

  // configurable application timezone (single timezone for all clinics)
  @Value("${app.timezone:Asia/Kolkata}")
  private String appTimezone;

  private ZoneId zoneId;

  public WeeklyTemplateService(WeeklyTemplateRepository templateRepo, SlotRepository slotRepo) {
    this.templateRepo = templateRepo;
    this.slotRepo = slotRepo;
  }

  @PostConstruct
  private void init() {
    this.zoneId = ZoneId.of(appTimezone);
  }

  public WeeklyTemplate createTemplate(WeeklyTemplateRequest req) {
    WeeklyTemplate t = new WeeklyTemplate();
    t.setDoctorId(req.getDoctorId());
    t.setSlotDurationMinutes(req.getSlotDurationMinutes());
    t.setActive(req.isActive());
    // map rules (if req.rules are already model.TemplateRule objects, adjust accordingly)
    List<com.divisha.model.TemplateRule> rules = new ArrayList<>();
    if (req.getRules() != null) {
      for (com.divisha.model.TemplateRule r : req.getRules()) {
        com.divisha.model.TemplateRule tr = new com.divisha.model.TemplateRule();
        tr.setDayOfWeek(r.getDayOfWeek());
        tr.setStart(r.getStart());
        tr.setEnd(r.getEnd());
        tr.setLocationId(r.getLocationId());
        rules.add(tr);
      }
    }
    t.setRules(rules);
    t.setCreatedAt(Instant.now());
    t.setUpdatedAt(Instant.now());
    return templateRepo.save(t);
  }

  public Optional<WeeklyTemplate> getTemplateById(String id) {
    return templateRepo.findById(id);
  }

  public List<WeeklyTemplate> listTemplates(String doctorId) {
    if (doctorId == null) {
      return templateRepo.findAll();
    } else {
      return templateRepo.findByDoctorId(doctorId);
    }
  }

  public WeeklyTemplate updateTemplate(String id, WeeklyTemplateRequest req) {
    WeeklyTemplate t =
        templateRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));

    t.setSlotDurationMinutes(req.getSlotDurationMinutes());
    t.setActive(req.isActive());
    t.setDoctorId(req.getDoctorId());

    List<TemplateRule> rules = new ArrayList<>();
    if (req.getRules() != null) {
      for (TemplateRule r : req.getRules()) {

        if (r.getStart() == null || r.getEnd() == null || !r.getStart().isBefore(r.getEnd())) {
          throw new IllegalArgumentException(
              "Invalid rule time range for location " + r.getLocationId());
        }

        TemplateRule tr = getTemplateRule(r);

        rules.add(tr);
      }
    }
    t.setRules(rules);
    t.setUpdatedAt(Instant.now());
    return templateRepo.save(t);
  }

  private static TemplateRule getTemplateRule(TemplateRule r) {
    TemplateRule tr = new TemplateRule();
    tr.setDayOfWeek(r.getDayOfWeek());
    tr.setStart(r.getStart());
    tr.setEnd(r.getEnd());
    tr.setLocationId(r.getLocationId());

    tr.setRecurrence(
        r.getRecurrence() == null ? TemplateRule.Recurrence.WEEKLY : r.getRecurrence());
    tr.setMonthOccurrence(r.getMonthOccurrence());

    // Extra check: if recurrence == MONTHLY, monthOccurrence must be present
    if (tr.getRecurrence() == TemplateRule.Recurrence.MONTHLY && tr.getMonthOccurrence() == null) {
      throw new IllegalArgumentException(
          "monthOccurrence is required for MONTHLY recurrence (location "
              + r.getLocationId()
              + ")");
    }
    return tr;
  }

  public void deleteTemplate(String id) {
    templateRepo.deleteById(id);
  }

  public void setActive(String id, boolean active) {
    WeeklyTemplate t =
        templateRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    t.setActive(active);
    t.setUpdatedAt(Instant.now());
    templateRepo.save(t);
  }

  /**
   * Generate slots for the template between from..to (inclusive). Uses a single application
   * timezone (zoneId) and batches inserts to improve performance.
   */
  @Transactional
  public void generateSlotsFromTemplate(String templateId, LocalDate from, LocalDate to) {
    WeeklyTemplate t =
        templateRepo
            .findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    if (!t.isActive()) return;

    int durationMinutes = t.getSlotDurationMinutes();
    if (durationMinutes <= 0) throw new IllegalArgumentException("Invalid slot duration");

    final int BATCH_SIZE = 500;
    List<Slot> toInsert = new ArrayList<>(BATCH_SIZE);

    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
      DayOfWeek dow = date.getDayOfWeek();

      // guard null rules
      if (t.getRules() == null) continue;

      for (com.divisha.model.TemplateRule rule : t.getRules()) {
        if (rule.getDayOfWeek() != dow) continue;

        // convert LocalTime -> ZonedDateTime using single app zone
        ZonedDateTime zonedStart = ZonedDateTime.of(date, rule.getStart(), zoneId);
        ZonedDateTime zonedEnd = ZonedDateTime.of(date, rule.getEnd(), zoneId);

        if (!zonedEnd.isAfter(zonedStart)) continue; // invalid rule

        // slice into slots
        for (ZonedDateTime cursor = zonedStart;
            !cursor.plusMinutes(durationMinutes).isAfter(zonedEnd);
            cursor = cursor.plusMinutes(durationMinutes)) {

          Instant slotStart = cursor.toInstant();
          Instant slotEnd = cursor.plusMinutes(durationMinutes).toInstant();

          // cheap existence check (ensure this method exists in SlotRepository)
          boolean exists =
              slotRepo.existsByDoctorIdAndLocationIdAndStart(
                  t.getDoctorId(), rule.getLocationId(), slotStart);

          if (!exists) {
            Slot s = new Slot();
            s.setDoctorId(t.getDoctorId());
            s.setLocationId(rule.getLocationId());
            s.setStart(slotStart);
            s.setEnd(slotEnd);
            s.setAvailable(true);
            toInsert.add(s);
          }

          if (toInsert.size() >= BATCH_SIZE) {
            slotRepo.saveAll(toInsert);
            toInsert.clear();
          }
        }
      }
    }

    if (!toInsert.isEmpty()) {
      slotRepo.saveAll(toInsert);
      toInsert.clear();
    }
  }
}
