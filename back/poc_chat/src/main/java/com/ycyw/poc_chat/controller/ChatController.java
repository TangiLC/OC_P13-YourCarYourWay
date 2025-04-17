package com.ycyw.poc_chat.controller;

import com.ycyw.poc_chat.dto.ChatMessageDto;
import com.ycyw.poc_chat.model.ChatMessage;
import com.ycyw.poc_chat.model.MessageType;
import com.ycyw.poc_chat.security.UserPrincipal;
import com.ycyw.poc_chat.service.DialogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "WebSocket Chat", description = "Endpoints WebSocket pour le chat")
public class ChatController {

  private final SimpMessagingTemplate messagingTemplate;
  private final DialogService dialogService;

  @SuppressWarnings("null")
  @Operation(
    summary = "Message d’un utilisateur : crée un dialog et le message"
  )
  @MessageMapping("/chat.sendMessage")
  public void sendUserMessage(
    ChatMessageDto message,
    SimpMessageHeaderAccessor headerAccessor
  ) {
    UserPrincipal sender = (UserPrincipal) headerAccessor
      .getSessionAttributes()
      .get("user");

    message.setSender(sender.getUsername());
    message.setTimestamp(LocalDateTime.now());
    message.setType(MessageType.CHAT);

    ChatMessage saved = dialogService.createDialogAndSendMessage(
      sender.getId(),
      message.getContent()
    );

    // Optionnel : broadcast public ou en privé
    messagingTemplate.convertAndSend("/topic/public", message);
  }

  @SuppressWarnings("null")
  @Operation(summary = "Répondre dans un dialogue existant")
  @MessageMapping("/chat.reply")
  public void replyToDialog(
    ChatMessageDto message,
    SimpMessageHeaderAccessor headerAccessor
  ) {
    UserPrincipal agent = (UserPrincipal) headerAccessor
      .getSessionAttributes()
      .get("user");

    message.setSender(agent.getUsername());
    message.setTimestamp(LocalDateTime.now());
    message.setType(MessageType.CHAT);

    ChatMessage saved = dialogService.agentSendMessage(
      message.getDialogId(),
      agent,
      message.getContent()
    );

    messagingTemplate.convertAndSend("/topic/public", message);
  }

  @SuppressWarnings("null")
  @Operation(summary = "Notification de connexion d’un utilisateur")
  @MessageMapping("/chat.addUser")
  public void addUser(
    ChatMessageDto message,
    SimpMessageHeaderAccessor headerAccessor
  ) {
    UserPrincipal user = (UserPrincipal) headerAccessor
      .getSessionAttributes()
      .get("user");

    message.setSender(user.getUsername());
    message.setTimestamp(LocalDateTime.now());
    message.setType(MessageType.JOIN);

    messagingTemplate.convertAndSend("/topic/public", message);
  }
}
