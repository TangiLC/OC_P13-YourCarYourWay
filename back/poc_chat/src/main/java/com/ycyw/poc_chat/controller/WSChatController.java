package com.ycyw.poc_chat.controller;

import com.ycyw.poc_chat.dto.ChatMessageDTO;
import com.ycyw.poc_chat.lifecycle.DialogLifecycleManager;
import com.ycyw.poc_chat.model.ChatMessage;
import com.ycyw.poc_chat.model.Dialog;
import com.ycyw.poc_chat.model.MessageType;
import com.ycyw.poc_chat.model.UserProfile;
import com.ycyw.poc_chat.repository.UserProfileRepository;
import com.ycyw.poc_chat.security.UserPrincipal;
import com.ycyw.poc_chat.service.DialogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WebSocket Chat", description = "Endpoints WebSocket pour le chat")
public class WSChatController {

  private final SimpMessagingTemplate messagingTemplate;
  private final DialogService dialogService;
  private final UserProfileRepository userProfileRepository;
  private final DialogLifecycleManager lifecycleManager;

  @Operation(summary = "Créer un nouveau salon de discussion (dialogue)")
  @MessageMapping("/chat.createDialog")
  public void createDialog(
    @Payload(required = false) String topic,
    SimpMessageHeaderAccessor headerAccessor
  ) {
    processAuthenticatedRequest(
      "createDialog",
      null,
      headerAccessor,
      (user, profile) -> {
        Dialog dialog = dialogService.createDialog(topic, user.getId());
        log.info(
          "Dialog {} created by user {}",
          dialog.getId(),
          user.getUsername()
        );
        Map<String, Object> payload = Map.of(
          "id",
          dialog.getId(),
          "topic",
          dialog.getTopic()
        );
        messagingTemplate.convertAndSendToUser(
          user.getUsername(),
          "/queue/dialog-created",
          payload
        );
      }
    );
  }

  @Operation(summary = "Envoyer un message dans un dialogue existant")
  @MessageMapping("/chat.sendMessage")
  public void sendUserMessage(
    ChatMessageDTO message,
    SimpMessageHeaderAccessor headerAccessor
  ) {
    processAuthenticatedRequest(
      "sendUserMessage",
      message,
      headerAccessor,
      (user, profile) -> {
        if (message.getDialogId() == null) {
          log.warn("sendUserMessage: dialogId is null");
          return;
        }
        prepareMessage(message, profile.getFirstName(), MessageType.CHAT);

        ChatMessage saved = dialogService.sendMessage(
          message.getDialogId(),
          user.getId(),
          message.getContent(),
          user.isClient()
        );

        message.setId(saved.getId());
        String destination = "/topic/dialog/" + message.getDialogId();
        messagingTemplate.convertAndSend(destination, message);
      }
    );
  }

  @Operation(summary = "Notification de connexion d'un utilisateur")
  @MessageMapping("/chat.addUser")
  public void addUser(
    ChatMessageDTO message,
    SimpMessageHeaderAccessor headerAccessor
  ) {
    processAuthenticatedRequest(
      "addUser",
      message,
      headerAccessor,
      (user, profile) -> {
        prepareMessage(message, profile.getFirstName(), MessageType.JOIN);
        String destination = "/topic/dialog/" + message.getDialogId();
        messagingTemplate.convertAndSend(destination, message);
        lifecycleManager.userJoined(message.getDialogId(), user.getUsername());
      }
    );
  }

  @Operation(
    summary = "Notification de déconnexion volontaire d'un utilisateur et fermeture du dialogue"
  )
  @MessageMapping("/chat.disconnect")
  public void disconnectUser(
    ChatMessageDTO message,
    SimpMessageHeaderAccessor headerAccessor
  ) {
    processAuthenticatedRequest(
      "disconnectUser",
      message,
      headerAccessor,
      (user, profile) -> {
        prepareMessage(message, profile.getFirstName(), MessageType.LEAVE);
        if (message.getDialogId() != null) {
          try {
            dialogService.closeDialog(message.getDialogId());
            log.info(
              "Dialog {} closed by user {}",
              message.getDialogId(),
              user.getUsername()
            );
          } catch (RuntimeException e) {
            log.warn("Cannot close dialog: {}", e.getMessage());
          }
        }
        String destination = "/topic/dialog/" + message.getDialogId();
        messagingTemplate.convertAndSend(destination, message);
        lifecycleManager.userLeft(message.getDialogId(), user.getUsername());
      }
    );
  }

  private void processAuthenticatedRequest(
    String methodName,
    ChatMessageDTO message,
    SimpMessageHeaderAccessor headerAccessor,
    BiConsumer<UserPrincipal, UserProfile> authenticatedAction
  ) {
    Authentication auth = (Authentication) headerAccessor.getUser();
    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
      log.warn("{}: utilisateur non authentifié", methodName);
      return;
    }
    UserPrincipal user = (UserPrincipal) auth.getPrincipal();

    UserProfile profile = userProfileRepository.findByUserCredentialId(
      user.getId()
    );
    if (profile == null) {
      log.warn(
        "{}: profil introuvable pour l'utilisateur {}",
        methodName,
        user.getId()
      );
      return;
    }

    authenticatedAction.accept(user, profile);
  }

  private void prepareMessage(
    ChatMessageDTO message,
    String senderName,
    MessageType messageType
  ) {
    message.setSender(senderName);
    message.setTimestamp(LocalDateTime.now());
    message.setType(messageType);
  }
}
