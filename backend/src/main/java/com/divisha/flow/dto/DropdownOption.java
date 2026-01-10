package com.divisha.flow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single option in a dropdown menu for WhatsApp Flow. This structure matches the
 * expected format for dropdown data sources.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropdownOption {

  @JsonProperty("id")
  private String id;

  @JsonProperty("title")
  private String title;

  @JsonProperty("description")
  private String description; // Optional description for the option
}
