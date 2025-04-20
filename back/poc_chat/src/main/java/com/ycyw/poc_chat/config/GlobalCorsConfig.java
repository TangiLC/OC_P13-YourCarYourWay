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
      .allowedOriginPatterns("*") 
      .allowedMethods("GET", "POST", "OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true);

    registry
      .addMapping("/ws/info/**")
      .allowedOriginPatterns("*")
      .allowedMethods("GET", "POST", "OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true);

      registry
      .addMapping("/api/**")
      .allowedOriginPatterns("http://localhost:4200")  
      .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true);
      
    // Configuration générale pour tous les autres endpoints
    registry
      .addMapping("/auth/**")
      .allowedOriginPatterns("http://localhost:4200")
      .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true);
  }
}
