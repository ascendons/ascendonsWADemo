package com.divisha.service;

import com.divisha.model.Location;
import com.divisha.repository.LocationRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
  private final LocationRepository locationRepository;

  @Value("${app.timezone:Asia/Kolkata}")
  private String timezone;

  public LocationService(LocationRepository locationRepository) {
    this.locationRepository = locationRepository;
  }

  public List<Location> getAllLocations() {
    return locationRepository.findAll();
  }

  public List<Location> getAllActiveLocation() {
    return locationRepository.findAll();
  }

  public Location createLocation(Location location) {
    location.setStatus("true");
    return locationRepository.save(location);
  }
}
