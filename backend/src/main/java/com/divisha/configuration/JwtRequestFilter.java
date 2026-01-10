package com.divisha.configuration;

import com.divisha.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Minimal JWT filter: validate token for configured path prefixes, skip login/register and OPTIONS.
 */
public class JwtRequestFilter extends OncePerRequestFilter {

  private final JwtTokenUtil jwtTokenUtil;

  // adjust prefixes as needed
  private final Set<String> protectedPrefixes =
      Set.of(
          "/api/patients",
          "/api/doctor",
          "/appointments",
          "/locations",
          "/api/auth" // keep /api/auth to protect user endpoints
          );

  // explicit excludes under /api/auth
  private final Set<String> excludedPaths =
      Set.of("/api/auth/login", "/api/auth/register", "/api/auth/ping");

  public JwtRequestFilter(JwtTokenUtil jwtTokenUtil) {
    this.jwtTokenUtil = jwtTokenUtil;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    // allow CORS preflight
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    // if excluded path, allow
    if (excludedPaths.contains(path)) {
      filterChain.doFilter(request, response);
      return;
    }

    // check if request matches any protected prefix
    boolean shouldProtect = protectedPrefixes.stream().anyMatch(path::startsWith);
    if (!shouldProtect) {
      filterChain.doFilter(request, response);
      return;
    }

    // now enforce token
    String authHeader = request.getHeader("Authorization");
    if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
      response.sendError(
          HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
      return;
    }

    String token = authHeader.substring(7);
    if (!jwtTokenUtil.validateToken(token)) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
      return;
    }

    // Optionally: attach subject/role to request attributes for controllers to read
    String subject = jwtTokenUtil.getSubject(token);
    String role = jwtTokenUtil.getRole(token);
    request.setAttribute("auth_subject", subject);
    request.setAttribute("auth_role", role);

    filterChain.doFilter(request, response);
  }
}
