package com.ycyw.poc_chat.security;

import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Intercepteur pour extraire et valider le token JWT lors de l'initialisation de la session WebSocket.
 */
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

  private final JwtTokenProvider jwtTokenProvider;

  public JwtHandshakeInterceptor(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @SuppressWarnings("null")
  @Override
  public boolean beforeHandshake(
    ServerHttpRequest request,
    ServerHttpResponse response,
    WebSocketHandler wsHandler,
    Map<String, Object> attributes
  ) {
    String token = request.getHeaders().getFirst("Authorization");
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
      if (jwtTokenProvider.validateToken(token)) {
        UserPrincipal user = jwtTokenProvider.getUserPrincipal(token);
        attributes.put("user", user);
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("null")
  @Override
  public void afterHandshake(
    ServerHttpRequest request,
    ServerHttpResponse response,
    WebSocketHandler wsHandler,
    Exception exception
  ) {
    
  }
}
