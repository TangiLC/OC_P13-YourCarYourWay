package com.ycyw.poc_chat.controller;

import com.ycyw.poc_chat.dto.DialogDTO;
import com.ycyw.poc_chat.mapper.DialogMapper;
import com.ycyw.poc_chat.model.Dialog;
import com.ycyw.poc_chat.model.DialogStatus;
import com.ycyw.poc_chat.repository.DialogRepository;
import com.ycyw.poc_chat.service.DialogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dialog")
@RequiredArgsConstructor
@Tag(
  name = "Chat API",
  description = "API pour le chat avec parallèle WebSocket"
)
public class DialogController {

  private final DialogRepository dialogRepository;
  private final DialogService dialogService;
  private final DialogMapper dialogMapper;

  @Operation(summary = "Récupérer tous les dialogues")
  @ApiResponse(
    responseCode = "200",
    description = "Liste des dialogues retournée",
    content = @Content(
      mediaType = "application/json",
      schema = @Schema(implementation = DialogDTO.class)
    )
  )
  @GetMapping("/all")
  public ResponseEntity<List<DialogDTO>> getAllDialogs() {
    Set<Dialog> dialogs = dialogRepository.findAllWithMessagesAndSenders();
    List<DialogDTO> dialogDTOs = dialogs
      .stream()
      .map(dialogMapper::toDialogDTO)
      .collect(Collectors.toList());
    return ResponseEntity.ok(dialogDTOs);
  }

  @Operation(
    summary = "Récupérer les dialogues par statut",
    description = "Statut possible : OPEN, PENDING, CLOSED"
  )
  @GetMapping("/status/{status}")
  public List<DialogDTO> getDialogsByStatus(
    @Parameter(
      description = "Statut du dialogue (OPEN, PENDING, CLOSED)",
      required = true
    ) @PathVariable DialogStatus status
  ) {
    return dialogRepository
      .findByStatus(status)
      .stream()
      .map(dialogMapper::toDialogDTO)
      .collect(Collectors.toList());
  }

  @Operation(summary = "Récupérer un dialogue par ID avec messages et senders")
  @GetMapping("/{id}")
  public ResponseEntity<DialogDTO> getDialogById(
    @Parameter(
      description = "ID du dialogue à récupérer",
      required = true
    ) @PathVariable Long id
  ) {
    return dialogRepository
      .findByIdWithMessagesAndSenders(id)
      .map(dialog -> ResponseEntity.ok(dialogMapper.toDialogDTO(dialog)))
      .orElse(ResponseEntity.notFound().build());
  }

  @Operation(
    summary = "Récupérer les dialogues par sender",
    description = "Renvoie les dialogues où l'utilisateur a posté un message"
  )
  @GetMapping("/sender/{id}")
  public Set<DialogDTO> getDialogsBySender(
    @Parameter(
      description = "ID du sender ayant posté des messages",
      required = true
    ) @PathVariable("id") Long id
  ) {
    return dialogRepository
      .findDistinctByParticipantsIdWithMessagesAndSenders(id)
      .stream()
      .map(dialogMapper::toDialogDTO)
      .collect(Collectors.toSet());
  }

  @Operation(
    summary = "Inviter un utilisateur dans un dialogue",
    description = "(rôle restreint AGENT/ADMIN)"
  )
  @ApiResponse(responseCode = "200", description = "Invitation envoyée")
  @ApiResponse(
    responseCode = "404",
    description = "Dialogue ou utilisateur introuvable"
  )
  @PostMapping("/{dialogId}/invite/{userId}")
  @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
  public ResponseEntity<Void> inviteUserToDialog(
    @PathVariable Long dialogId,
    @PathVariable Long userId
  ) {
    try {
      dialogService.inviteUser(dialogId, userId);
      return ResponseEntity.ok().build();
    } catch (EntityNotFoundException ex) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(
    summary = "Marquer tous les messages comme lus pour un utilisateur",
    description = "Marque comme lus tous les messages d'un dialogue dont l'expéditeur n'est pas l'utilisateur spécifié"
  )
  @ApiResponse(responseCode = "200", description = "Messages marqués comme lus")
  @ApiResponse(responseCode = "404", description = "Dialogue introuvable")
  @PostMapping("/{dialogId}/{senderId}/markasread")
  public ResponseEntity<Void> markMessagesAsRead(
    @Parameter(
      description = "ID du dialogue",
      required = true
    ) @PathVariable Long dialogId,
    @Parameter(
      description = "ID de l'utilisateur qui lit les messages",
      required = true
    ) @PathVariable Long senderId
  ) {
    try {
      dialogService.markMessagesAsRead(dialogId, senderId);
      return ResponseEntity.ok().build();
    } catch (EntityNotFoundException ex) {
      return ResponseEntity.notFound().build();
    }
  }
}
