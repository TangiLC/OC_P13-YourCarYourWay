package com.ycyw.poc_chat.config;

import com.ycyw.poc_chat.security.JwtTokenProvider;
import com.ycyw.poc_chat.security.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtTokenProvider jwtTokenProvider;
  private final WebSocketAuthInterceptor webSocketAuthInterceptor;

  public WebSocketConfig(
    JwtTokenProvider jwtTokenProvider,
    WebSocketAuthInterceptor webSocketAuthInterceptor
  ) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.webSocketAuthInterceptor = webSocketAuthInterceptor;
  }

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
      //.addInterceptors(new JwtHandshakeInterceptor(jwtTokenProvider))
      .setAllowedOriginPatterns("*")
      .withSockJS()
      .setSuppressCors(true);
  }

  @SuppressWarnings("null")
  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(webSocketAuthInterceptor);
  }
}
