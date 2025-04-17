package com.ycyw.poc_chat.service;

import com.ycyw.poc_chat.model.*;
import com.ycyw.poc_chat.repository.*;
import com.ycyw.poc_chat.security.UserPrincipal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service métier pour la gestion des dialogues et des messages associés.
 */
@Service
@RequiredArgsConstructor
public class DialogService {

  private final DialogRepository dialogRepository;
  private final UserProfileRepository userProfileRepository;
  private final ChatMessageRepository messageRepository;

  /**
   * Crée un dialogue (si inexistant), associe l'utilisateur, enregistre le message.
   *
   * @param userId     identifiant du UserProfile émetteur
   * @param content    contenu du message
   * @param topic      titre du dialogue, vérification de l'unicité et non-nullité
   *                   //topic null en cas de chat en cours
   * @return le message sauvegardé
   */
  @Transactional
  public ChatMessage createDialogAndSendMessage(Long userId, String content) {
    return createDialogAndSendMessage(userId, content, null);
  }

  @Transactional
  public ChatMessage createDialogAndSendMessage(
    Long userId,
    String content,
    String topic
  ) {
    UserProfile user = userProfileRepository
      .findById(userId)
      .orElseThrow(() -> new RuntimeException("User not found"));
    // Cherche un dialogue ouvert sans agent encore lié
    Optional<Dialog> existing = dialogRepository.findFirstByParticipantsContainingAndStatus(
      user,
      DialogStatus.OPEN
    );
    Dialog dialog = existing.orElseGet(() -> {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss"
      );
      String timestamp = LocalDateTime.now().format(formatter);
      String finalTopic = topic;
      if (topic == null || topic.trim().isEmpty()) {
        finalTopic = "New Messages_" + timestamp;
      } else {
        List<Dialog> userDialogs = dialogRepository.findByParticipantsContainingOrderByCreatedAtDesc(
          user
        );
        boolean topicExists = userDialogs
          .stream()
          .anyMatch(d -> topic.equals(d.getTopic()));
        if (topicExists) {
          finalTopic = topic + " (" + timestamp + ")";
        }
      }
      Dialog newDialog = Dialog
        .builder()
        .createdAt(LocalDateTime.now())
        .status(DialogStatus.OPEN)
        .topic(finalTopic)
        .build();
      newDialog.getParticipants().add(user);
      return dialogRepository.save(newDialog);
    });

    ChatMessage message = ChatMessage
      .builder()
      .sender(user)
      .timestamp(LocalDateTime.now())
      .content(content)
      .type(MessageType.CHAT)
      .dialog(dialog)
      .build();
    return messageRepository.save(message);
  }

  /**
   * Associe un agent à un dialogue (ex: après notification ou sélection).
   */
  @Transactional
  public void assignAgentToDialog(Long dialogId, Long agentProfileId) {
    Dialog dialog = dialogRepository
      .findById(dialogId)
      .orElseThrow(() -> new RuntimeException("Dialog not found"));
    UserProfile agent = userProfileRepository
      .findById(agentProfileId)
      .orElseThrow(() -> new RuntimeException("Agent not found"));
    dialog.getParticipants().add(agent);
    dialogRepository.save(dialog);
  }

  /**
   * Enregistre une réponse d'agent dans un dialogue existant.
   */
  @Transactional
  public ChatMessage agentSendMessage(
    Long dialogId,
    UserPrincipal agentPrincipal,
    String content
  ) {
    Dialog dialog = dialogRepository
      .findById(dialogId)
      .orElseThrow(() -> new RuntimeException("Dialog not found"));
    UserProfile agent = userProfileRepository
      .findById(agentPrincipal.getId())
      .orElseThrow(() -> new RuntimeException("Agent not found"));
    if (!dialog.getParticipants().contains(agent)) {
      dialog.getParticipants().add(agent);
    }
    ChatMessage response = ChatMessage
      .builder()
      .sender(agent) // Utilisation de l'objet UserProfile comme sender
      .content(content)
      .timestamp(LocalDateTime.now())
      .type(MessageType.CHAT)
      .dialog(dialog)
      .build();
    return messageRepository.save(response);
  }
}
