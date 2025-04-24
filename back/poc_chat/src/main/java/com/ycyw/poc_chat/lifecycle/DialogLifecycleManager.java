package com.ycyw.poc_chat.lifecycle;

import com.ycyw.poc_chat.model.Dialog;
import com.ycyw.poc_chat.model.DialogStatus;
import com.ycyw.poc_chat.repository.DialogRepository;
import com.ycyw.poc_chat.service.DialogService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class DialogLifecycleManager {

  private final DialogRepository dialogRepository;
  private final DialogService dialogService;
  private final SimpMessagingTemplate messagingTemplate;
  private final ConcurrentMap<Long, Set<String>> activeSessions = new ConcurrentHashMap<>();
  private final Set<Long> warnedDialogs = ConcurrentHashMap.newKeySet();

  public void userJoined(Long dialogId, String username) {
    Set<String> sessions = activeSessions.computeIfAbsent(
      dialogId,
      id -> ConcurrentHashMap.newKeySet()
    );
    sessions.add(username);

    Dialog dialog = dialogRepository.findById(dialogId).orElseThrow();
    if (dialog.getStatus() == DialogStatus.PENDING && sessions.size() > 1) {
      dialog.setStatus(DialogStatus.OPEN);
      dialogRepository.save(dialog);
      System.out.println("Tentative d'envoi : OPEN");
    }
    messagingTemplate.convertAndSend("/topic/dialogs/update", "OPEN");
  }

  public void userLeft(Long dialogId, String username) {
    Set<String> sessions = activeSessions.get(dialogId);
    if (sessions != null) {
      sessions.remove(username);

      Dialog dialog = dialogRepository.findById(dialogId).orElseThrow();
      if (dialog.getStatus() == DialogStatus.OPEN && sessions.size() < 2) {
        dialog.setStatus(DialogStatus.CLOSED);
        dialog.setClosedAt(LocalDateTime.now());
        dialogRepository.save(dialog);
        messagingTemplate.convertAndSend(
          "/topic/dialog/" + dialogId,
          Map.of("type", "CLOSE", "dialogId", dialogId)
        );
        messagingTemplate.convertAndSend("/topic/dialogs/update", "CLOSED");
      }
    }
  }

  public void messageSent(Long dialogId) {
    Set<String> sessions = activeSessions.getOrDefault(dialogId, Set.of());

    Dialog dialog = dialogRepository.findById(dialogId).orElseThrow();

    if (dialog.getStatus() == DialogStatus.CLOSED) {
      dialog.setStatus(DialogStatus.PENDING);
      dialog.setClosedAt(null);
      dialogRepository.save(dialog);
    }
    messagingTemplate.convertAndSend("/topic/dialogs/update", "PENDING");
  }

  /**
   * Toutes les 10 minutes :
   * - À 49 min d’inactivité, on prévient une seule fois.
   * - À 59 min d’inactivité, on ferme le salon.
   */
  @Scheduled(fixedRate = 600_000)
  public void manageInactiveDialogs() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime warnThreshold = now.minusMinutes(49);
    LocalDateTime closeThreshold = now.minusMinutes(59);

    dialogRepository
      .findByStatus(DialogStatus.OPEN)
      .forEach(dialog -> {
        Long id = dialog.getId();
        LocalDateTime last = dialog.getLastActivityAt();

        if (last.isBefore(warnThreshold) && !warnedDialogs.contains(id)) {
          messagingTemplate.convertAndSend(
            "/topic/dialog/" + id,
            Map.of(
              "type",
              "INFO",
              "message",
              "Le salon est inactif et va fermer dans 10 min."
            )
          );
          warnedDialogs.add(id);
        }

        if (last.isBefore(closeThreshold)) {
          dialogService.closeDialog(id);
          messagingTemplate.convertAndSend(
            "/topic/dialog/" + id,
            Map.of("type", "CLOSE", "dialogId", id)
          );
          warnedDialogs.remove(id);
        }
      });
  }
}
