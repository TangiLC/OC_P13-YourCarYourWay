package com.ycyw.poc_chat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtre qui intercepte chaque requête pour y valider le JWT.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(
    JwtAuthenticationFilter.class
  );

  @Autowired
  private JwtTokenProvider tokenProvider;

  @Autowired
  private CustomUserDetailsService userDetailsService;

  @SuppressWarnings("null")
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request)
    throws ServletException {
    String path = request.getServletPath();
    return (
      path.startsWith("/auth") ||
      path.startsWith("/swagger-ui") ||
      path.startsWith("/v3/api-docs")
    );
  }

  @SuppressWarnings("null")
  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    String header = request.getHeader("Authorization");

    try {
      if (header != null && header.startsWith("Bearer ")) {
        String token = header.substring(7);
        if (tokenProvider.validateToken(token)) {
          Long userId = tokenProvider.getUserIdFromToken(token);
          UserDetails userDetails = userDetailsService.loadUserById(userId);
          UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
          );
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      }
    } catch (Exception ex) {
      logger.warn("Échec de l'authentification JWT : {}", ex.getMessage());
    }

    filterChain.doFilter(request, response);
  }
}
