package com.divisha;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DivishaBotApplication {

  @PostConstruct
  public void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    System.setProperty("user.timezone", "Asia/Kolkata");
  }

  public static void main(String[] args) {
    SpringApplication.run(DivishaBotApplication.class, args);
  }
}
