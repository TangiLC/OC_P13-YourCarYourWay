package com.ycyw.poc_chat.security;

//import com.ycyw.poc_chat.security.JwtService;
import com.ycyw.poc_chat.security.UserPrincipal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  private final JwtTokenProvider jwtService;

  public WebSocketAuthInterceptor(JwtTokenProvider jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
      message,
      StompHeaderAccessor.class
    );

    if (
      accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())
    ) {
      String authorization = accessor.getFirstNativeHeader("Authorization");

      if (authorization != null && authorization.startsWith("Bearer ")) {
        String token = authorization.substring(7);

        try {
          UserPrincipal user = jwtService.getUserPrincipal(token);
          UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            user,
            null,
            user.getAuthorities()
          );

          accessor.setUser(auth);
          accessor.getSessionAttributes().put("user", user);
        } catch (Exception e) {
          // Token invalide, l'utilisateur ne sera pas authentifié
        }
      }
    }
    return message;
  }
}
