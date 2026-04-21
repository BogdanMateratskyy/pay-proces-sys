package com.bmsoftware.payment.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationTest {

  @Autowired private MockMvc mockMvc;

  private final String secret = "mysecretkeymysecretkeymysecretkeymysecretkey";

  @Test
  void shouldReturnUnauthorizedWhenNoToken() throws Exception {
    mockMvc.perform(get("/api/v1/test")).andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnOkWhenValidToken() throws Exception {
    String token =
        Jwts.builder()
            .subject("testuser")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(
                Keys.hmacShaKeyFor(
                    java.util.Base64.getDecoder().decode(secret.getBytes(StandardCharsets.UTF_8))))
            .compact();

    mockMvc
        .perform(get("/api/v1/test").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().string("Authenticated"));
  }

  @Test
  void shouldReturnUnauthorizedWhenInvalidToken() throws Exception {
    mockMvc
        .perform(get("/api/v1/test").header("Authorization", "Bearer invalidtoken"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnUnauthorizedWhenExpiredToken() throws Exception {
    String token =
        Jwts.builder()
            .subject("testuser")
            .issuedAt(new Date(System.currentTimeMillis() - 7200000))
            .expiration(new Date(System.currentTimeMillis() - 3600000))
            .signWith(
                Keys.hmacShaKeyFor(
                    java.util.Base64.getDecoder().decode(secret.getBytes(StandardCharsets.UTF_8))))
            .compact();

    mockMvc
        .perform(get("/api/v1/test").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }
}
