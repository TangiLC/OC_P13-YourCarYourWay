package com.ycyw.poc_chat.config;

import com.ycyw.poc_chat.security.JwtHandshakeInterceptor;
import com.ycyw.poc_chat.security.JwtTokenProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
/**
 * Configuration WebSocket avec STOMP et SockJS.
 */
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private JwtTokenProvider jwtTokenProvider;

  @SuppressWarnings("null")
  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic", "/queue");
    config.setApplicationDestinationPrefixes("/app");
    config.setUserDestinationPrefix("/user");
  }

  @SuppressWarnings("null")
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
      .addEndpoint("/ws")
      .addInterceptors(new JwtHandshakeInterceptor(jwtTokenProvider))
      .setAllowedOriginPatterns("*")
      .withSockJS();
  }
}
