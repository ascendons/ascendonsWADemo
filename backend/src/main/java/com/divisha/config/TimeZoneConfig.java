package com.divisha.config;

import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeZoneConfig {

  public static final String TIMEZONE = "Asia/Kolkata";
  public static final ZoneId ZONE_ID = ZoneId.of(TIMEZONE);

  @Bean
  public ZoneId zoneId() {
    return ZONE_ID;
  }
}
