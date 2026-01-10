package com.divisha.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenUtil {
  private static final String SECRET =
      "ThisIsASecretKeyForJwtDivishaApp123456789"; // move to env in prod
  private static final long EXPIRATION_MS = 1000 * 60 * 60 * 8; // 8 hours

  private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

  // subject: user id or email. role: e.g. "ROLE_USER"
  public String generateToken(String subject, String role) {
    return Jwts.builder()
        .setSubject(subject)
        .claim("role", role)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public String getSubject(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  public String getRole(String token) {
    return (String)
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("role");
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}
