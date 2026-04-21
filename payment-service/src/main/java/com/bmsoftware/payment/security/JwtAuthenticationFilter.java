package com.bmsoftware.payment.security;

import com.bmsoftware.shared.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  public static final String BEARER_TOKEN_PREFIX = "Bearer ";
  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith(BEARER_TOKEN_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    final String jwt = authHeader.substring(7);
    if (jwtTokenProvider.isTokenValid(jwt)) {
      final String username = jwtTokenProvider.extractUsername(jwt);
      if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        var authToken =
            new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }

    filterChain.doFilter(request, response);
  }
}
