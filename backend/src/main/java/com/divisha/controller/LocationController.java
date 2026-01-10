package com.divisha.controller;

import com.divisha.model.Location;
import com.divisha.service.LocationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/locations")
public class LocationController {
  private final LocationService locationService;

  LocationController(LocationService locationService) {
    this.locationService = locationService;
  }

  @GetMapping
  public ResponseEntity<List<Location>> getAllLocations() {
    return ResponseEntity.ok(locationService.getAllLocations());
  }

  @PostMapping
  public ResponseEntity<Location> createLocation(@RequestBody Location location) {
    return ResponseEntity.ok(locationService.createLocation(location));
  }
}
