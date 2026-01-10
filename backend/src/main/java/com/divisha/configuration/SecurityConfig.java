package com.divisha.configuration;

import com.divisha.util.JwtTokenUtil;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  /**
   * Use a delegating encoder (supports {bcrypt}, {noop}, {argon2}, ...) Keeps you flexible and
   * avoids having two PasswordEncoder beans of the same type.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  /** Register JwtRequestFilter as a bean so it can be injected into the security chain. */
  @Bean
  public JwtRequestFilter jwtRequestFilter(JwtTokenUtil jwtTokenUtil) {
    return new JwtRequestFilter(jwtTokenUtil);
  }

  /**
   * Primary security filter chain: enable CORS support here so Spring Security won't short-circuit
   * preflight requests. We add the JwtRequestFilter before UsernamePasswordAuthenticationFilter so
   * it runs early and enforces tokens for the configured paths.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {
    http.cors() // IMPORTANT: enable CORS support
        .and()
        .csrf(csrf -> csrf.disable())
        // keep permitAll() — the filter enforces JWT protection for the selected paths
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable());

    // register our filter early in the chain
    http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * CORS configuration source consulted by Spring Security when handling requests. Adjust
   * allowedOrigins for production.
   *
   * <p>NOTE: we expose the Authorization header so the frontend can read the token returned by
   * /api/auth/login (you set it on the Response headers in your controller).
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of("http://localhost:8080")); // react dev server
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setExposedHeaders(
        List.of("Authorization")); // allow frontend JS to read Authorization header
    cfg.setAllowCredentials(true); // if your frontend sends cookies/auth headers
    cfg.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }
}
