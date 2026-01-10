package com.divisha.model;

import lombok.Data;

@Data
public class LocationCreateRequest {
  private String code;
  private String name;
  private String address;
}
