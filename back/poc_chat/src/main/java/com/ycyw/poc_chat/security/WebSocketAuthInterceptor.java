package com.ycyw.poc_chat.security;

import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Intercepteur STOMP qui valide le token JWT lors de la commande CONNECT
 * et place l'objet Authentication dans le contexte du message.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  private final JwtTokenProvider jwtTokenProvider;

  public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @SuppressWarnings("null")
  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
      message,
      StompHeaderAccessor.class
    );

    if (
      accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())
    ) {
      List<String> authHeaders = accessor.getNativeHeader("Authorization");
      if (authHeaders != null && !authHeaders.isEmpty()) {
        String bearer = authHeaders.get(0);
        if (bearer.startsWith("Bearer ")) {
          String token = bearer.substring(7);
          try {
            if (jwtTokenProvider.validateToken(token)) {
              UserPrincipal user = jwtTokenProvider.getUserPrincipal(token);
              Authentication auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
              );
              accessor.setUser(auth);
            }
          } catch (Exception ex) {}
        }
      }
    }
    return message;
  }
}
