package com.divisha.controller;

import com.divisha.service.SlotGenerationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateSlotController {

  private final SlotGenerationService slotGenerationService;

  public static class GenerateSlotsRequest {
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    public LocalDate startDate;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    public LocalDate endDate;
  }

  public static class GenerateSlotsResponse {
    public String templateId;
    public LocalDate startDate;
    public LocalDate endDate;
    public int slotsCreated;
    public String message;

    public GenerateSlotsResponse(
        String templateId,
        LocalDate startDate,
        LocalDate endDate,
        int slotsCreated,
        String message) {
      this.templateId = templateId;
      this.startDate = startDate;
      this.endDate = endDate;
      this.slotsCreated = slotsCreated;
      this.message = message;
    }
  }

  @PostMapping("/{templateId}/generate-slots")
  public ResponseEntity<?> generateSlots(
      @PathVariable("templateId") String templateId, @Valid @RequestBody GenerateSlotsRequest req) {
    // basic sanity checks
    if (req.startDate.isAfter(req.endDate)) {
      return ResponseEntity.badRequest().body("startDate must be on or before endDate");
    }
    if (req.endDate.isAfter(req.startDate.plusDays(30))) {
      return ResponseEntity.badRequest()
          .body("endDate cannot be more than 30 days after startDate");
    }

    int created =
        slotGenerationService.generateSlotsForTemplate(templateId, req.startDate, req.endDate);

    GenerateSlotsResponse resp =
        new GenerateSlotsResponse(
            templateId,
            req.startDate,
            req.endDate,
            created,
            created > 0
                ? "Slots generated"
                : "No new slots created (maybe duplicates or inactive template)");

    return ResponseEntity.ok(resp);
  }
}
