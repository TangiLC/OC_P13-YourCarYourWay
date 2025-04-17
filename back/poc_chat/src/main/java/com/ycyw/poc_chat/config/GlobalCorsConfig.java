package com.ycyw.poc_chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration CORS globale pour les appels SockJS.
 */
@Configuration
public class GlobalCorsConfig implements WebMvcConfigurer {

  @SuppressWarnings("null")
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
      .addMapping("/ws/**")
      .allowedOriginPatterns("*") // ou liste précise des origins (ex : http://localhost:5500)
      .allowedMethods("GET", "POST", "OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true);

    registry
      .addMapping("/ws/info/**")
      .allowedOriginPatterns("*")
      .allowedMethods("GET", "POST", "OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true);
  }
}
