package com.divisha.model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetaListRow {
  private String id; // Unique row ID
  private String title; // Row title
  private String description; // Row description (optional)
}
