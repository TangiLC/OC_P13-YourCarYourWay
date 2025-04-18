package com.ycyw.poc_chat.controller;

import com.ycyw.poc_chat.dto.ChatMessageDTO;
import com.ycyw.poc_chat.model.ChatMessage;
import com.ycyw.poc_chat.model.Dialog;
import com.ycyw.poc_chat.model.DialogStatus;
import com.ycyw.poc_chat.model.MessageType;
import com.ycyw.poc_chat.model.UserProfile;
import com.ycyw.poc_chat.repository.UserProfileRepository;
import com.ycyw.poc_chat.security.UserPrincipal;
import com.ycyw.poc_chat.service.DialogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WebSocket Chat", description = "Endpoints WebSocket pour le chat")
public class ChatController {

  private final SimpMessagingTemplate messagingTemplate;
  private final DialogService dialogService;
  private final UserProfileRepository userProfileRepository;

  @Operation(
    summary = "Message d'un utilisateur : crée un dialog et le message"
  )
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
        prepareMessage(message, profile.getFirstName(), MessageType.CHAT);

        ChatMessage saved = dialogService.createDialogAndSendMessage(
          user.getId(),
          message.getContent()
        );

        message.setId(saved.getId());
        message.setDialogId(saved.getDialog().getId());

        log.info(
          "Broadcasting message '{}' from {}",
          message.getContent(),
          user.getUsername()
        );
        messagingTemplate.convertAndSend("/topic/public", message);
      }
    );
  }

  @Operation(summary = "Répondre dans un dialogue existant")
  @MessageMapping("/chat.reply")
  public void replyToDialog(
    ChatMessageDTO message,
    SimpMessageHeaderAccessor headerAccessor
  ) {
    processAuthenticatedRequest(
      "replyToDialog",
      message,
      headerAccessor,
      (agent, profile) -> {
        prepareMessage(message, profile.getFirstName(), MessageType.CHAT);

        ChatMessage saved = dialogService.agentSendMessage(
          message.getDialogId(),
          agent,
          message.getContent()
        );

        message.setId(saved.getId());

        log.info(
          "Agent '{}' replied in dialog {}",
          agent.getUsername(),
          message.getDialogId()
        );
        messagingTemplate.convertAndSend("/topic/public", message);
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

        log.info("User '{}' joined", user.getUsername());
        messagingTemplate.convertAndSend("/topic/public", message);
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

        log.info("User '{}' explicitly disconnected", user.getUsername());
        messagingTemplate.convertAndSend("/topic/public", message);
      }
    );
  }

  /**
   * Traite une requête authentifiée en gérant les vérifications d'authentification et de profil
   * @param methodName Le nom de la méthode appelante (pour les logs)
   * @param message Le message à traiter
   * @param headerAccessor Les en-têtes du message
   * @param authenticatedAction L'action à exécuter si l'utilisateur est authentifié et a un profil
   */
  private void processAuthenticatedRequest(
    String methodName,
    ChatMessageDTO message,
    SimpMessageHeaderAccessor headerAccessor,
    BiConsumer<UserPrincipal, UserProfile> authenticatedAction
  ) {
    // Vérification de l'authentification
    UserPrincipal user = getAuthenticatedUser(headerAccessor, methodName);
    if (user == null) {
      return;
    }

    // Récupération du profil
    UserProfile profile = getUserProfile(user.getId(), methodName);
    if (profile == null) {
      return;
    }

    // Exécution de l'action avec l'utilisateur et le profil
    authenticatedAction.accept(user, profile);
  }

  /**
   * Récupère l'utilisateur authentifié à partir des en-têtes
   * @param headerAccessor Les en-têtes du message
   * @param methodName Le nom de la méthode appelante (pour les logs)
   * @return L'utilisateur authentifié ou null si non authentifié
   */
  private UserPrincipal getAuthenticatedUser(
    SimpMessageHeaderAccessor headerAccessor,
    String methodName
  ) {
    Authentication auth = (Authentication) headerAccessor.getUser();
    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
      log.warn("{}: utilisateur non authentifié", methodName);
      return null;
    }
    return (UserPrincipal) auth.getPrincipal();
  }

  /**
   * Récupère le profil utilisateur à partir de l'ID
   * @param userId L'ID de l'utilisateur
   * @param methodName Le nom de la méthode appelante (pour les logs)
   * @return Le profil utilisateur ou null si non trouvé
   */
  private UserProfile getUserProfile(Long userId, String methodName) {
    UserProfile profile = userProfileRepository.findByUserCredentialId(userId);
    if (profile == null) {
      log.warn(
        "{}: profil introuvable pour l'utilisateur {}",
        methodName,
        userId
      );
      return null;
    }
    return profile;
  }

  /**
   * Prépare un message avec les informations communes
   * @param message Le message à préparer
   * @param senderName Le nom de l'expéditeur
   * @param messageType Le type de message
   */
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
