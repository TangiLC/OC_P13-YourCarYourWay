package com.ycyw.poc_chat.service;

import com.ycyw.poc_chat.model.*;
import com.ycyw.poc_chat.repository.*;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DialogService {

  private final DialogRepository dialogRepository;
  private final UserProfileRepository userProfileRepository;
  private final ChatMessageRepository messageRepository;
  private final SimpMessagingTemplate messagingTemplate;

  /**
+   * Crée un nouveau dialogue pour un utilisateur donné.
+   *
+   * @param topic        sujet du dialogue
+   * @param requesterId  ID de l’utilisateur (récupéré en amont)
+   * @return le dialogue créé
+   */
  @Transactional
  public Dialog createDialog(String topic, Long requesterId) {
    /*Authentication auth = SecurityContextHolder
      .getContext()
      .getAuthentication();
    UserPrincipal requester = (UserPrincipal) auth.getPrincipal();*/

    String timestamp = LocalDateTime
      .now()
      .format(DateTimeFormatter.ofPattern("yyyyMMdd_HH:mm:ss"));

    final String finalTopic;
    if (topic == null || topic.isBlank()) {
      String uuid = UUID.randomUUID().toString();
      finalTopic = "Chat_" + uuid + ".@" + timestamp;
    } else {
      finalTopic = topic + ".@" + timestamp;
    }

    Dialog dialog = Dialog
      .builder()
      .topic(finalTopic)
      .status(DialogStatus.PENDING)
      .createdAt(LocalDateTime.now())
      .build();

    if (dialog.getParticipants() == null) {
      dialog.setParticipants(new HashSet<>());
    }

    UserProfile clientProfile = UserProfile.builder().id(requesterId).build();
    dialog.getParticipants().add(clientProfile);

    return dialogRepository.save(dialog);
  }

  /**
   * Envoie un message dans un dialogue existant.
   *
   * @param dialogId ID du dialogue
   * @param senderId ID de l'utilisateur expéditeur
   * @param content  contenu du message
   * @return le message enregistré
   */
  @Transactional
  public ChatMessage sendMessage(
    Long dialogId,
    Long senderId,
    String content,
    boolean isClient
  ) {
    UserProfile senderProfile;
    Dialog dialog = dialogRepository
      .findById(dialogId)
      .orElseThrow(() -> new RuntimeException("Dialog not found"));

    if (isClient) {
      if (dialog.getStatus() == DialogStatus.CLOSED) {
        dialog.setStatus(DialogStatus.PENDING);
        dialog.setClosedAt(null);
      }
      senderProfile = UserProfile.builder().id(senderId).build();
    } else {
      senderProfile = userProfileRepository.getReferenceById(senderId);
      if (dialog.getStatus() == DialogStatus.PENDING) {
        dialog.setStatus(DialogStatus.OPEN);
      }
    }

    dialog.setLastActivityAt(LocalDateTime.now());

    if (
      dialog
        .getParticipants()
        .stream()
        .noneMatch(p -> p.getId().equals(senderId))
    ) {
      dialog.getParticipants().add(senderProfile);
    }

    ChatMessage message = ChatMessage
      .builder()
      .sender(senderProfile)
      .timestamp(LocalDateTime.now())
      .content(content)
      .type(MessageType.CHAT)
      .dialog(dialog)
      .build();

    return messageRepository.save(message);
  }

  /**
   * Ferme un dialogue (change son statut).
   */
  @Transactional
  public Dialog closeDialog(Long dialogId) {
    Dialog dialog = dialogRepository
      .findById(dialogId)
      .orElseThrow(() -> new RuntimeException("Dialog not found"));

    if (dialog.getStatus() == DialogStatus.CLOSED) {
      throw new RuntimeException("Dialog already closed");
    }
    dialog.setStatus(DialogStatus.CLOSED);
    dialog.setClosedAt(LocalDateTime.now());

    return dialogRepository.save(dialog);
  }

  /**
   * Ajoute un participant à un dialogue existant.
   */
  @Transactional
  public void inviteUser(Long dialogId, Long userId) {
    Dialog dialog = dialogRepository
      .findById(dialogId)
      .orElseThrow(() -> new EntityNotFoundException("Dialog not found"));

    UserProfile invitee = userProfileRepository
      .findById(userId)
      .orElseThrow(() -> new EntityNotFoundException("User not found"));

    if (!dialog.getParticipants().contains(invitee)) {
      dialog.getParticipants().add(invitee);
      dialog.setLastActivityAt(LocalDateTime.now());
      dialogRepository.save(dialog);

      messagingTemplate.convertAndSend(
        "/topic/dialog/" + dialogId + "/invites",
        Map.of("dialogId", dialogId, "invitedUserId", userId)
      );
    }
  }
}
