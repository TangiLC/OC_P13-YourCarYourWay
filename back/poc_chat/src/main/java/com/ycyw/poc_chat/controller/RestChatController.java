package com.ycyw.poc_chat.controller;

import com.ycyw.poc_chat.dto.ChatMessageDTO;
import com.ycyw.poc_chat.dto.DialogResponseDTO;
import com.ycyw.poc_chat.model.ChatMessage;
import com.ycyw.poc_chat.model.Dialog;
import com.ycyw.poc_chat.security.UserPrincipal;
import com.ycyw.poc_chat.service.DialogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * API REST pour gérer les salons et messages de chat,
 *  en parallèle de la messagerie WebSocket.
 */
@RestController
@RequestMapping("/api/dialog")
@RequiredArgsConstructor
@Tag(
  name = "Chat API",
  description = "API pour le chat avec parallèle WebSocket"
)
public class RestChatController {

  private final DialogService dialogService;
  private final SimpMessagingTemplate messagingTemplate;

  /**
   * Crée un nouveau salon de discussion.
   *
   * @param topic Sujet du salon, optionnel. Si null ou vide, un topic par défaut est généré.
   * @param authentication Contexte d'authentification Spring Security.
   * @return Map contenant l'identifiant et le topic du salon créé.
   */
  @Operation(
    summary = "Créer un nouveau salon de discussion",
    description = "Crée un salon avec un topic optionnel. Retourne l'ID et le topic."
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "200",
        description = "Salon créé avec succès"
      ),
      @ApiResponse(responseCode = "401", description = "Non authentifié"),
    }
  )
  @PostMapping("/")
  public ResponseEntity<DialogResponseDTO> createDialog(
    @Parameter(
      description = "Sujet du salon, optionnel (un timestamp est créé si topic vide)",
      required = false
    ) @RequestParam(required = false) String topic,
    Authentication authentication
  ) {
    UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
    Dialog dialog = dialogService.createDialog(topic, user.getId());

    DialogResponseDTO response = new DialogResponseDTO(
      dialog.getId(),
      dialog.getTopic()
    );

    messagingTemplate.convertAndSendToUser(
      user.getUsername(),
      "/queue/dialog-created",
      response
    );

    return ResponseEntity.ok(response);
  }

  /**
   * Envoie un message dans un salon existant.
   *
   * @param dialogId ID du salon.
   * @param body Corps de la requête JSON, clé "content" pour le message.
   * @param authentication Contexte d'authentification Spring Security.
   * @return DTO du message envoyé.
   */
  @Operation(
    summary = "Envoyer un message dans un dialogue existant",
    description = "Enregistre un message et notifie les participants via WebSocket."
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "200",
        description = "Message envoyé avec succès"
      ),
      @ApiResponse(responseCode = "401", description = "Non authentifié"),
      @ApiResponse(responseCode = "404", description = "Dialogue non trouvé"),
    }
  )
  @PostMapping("/{dialogId}/message")
  public ResponseEntity<ChatMessageDTO> sendMessage(
    @Parameter(
      description = "ID du dialogue",
      required = true
    ) @PathVariable Long dialogId,
    @RequestBody(
      description = "Contenu du message à envoyer",
      required = true
    ) @org.springframework.web.bind.annotation.RequestBody Map<String, String> body,
    Authentication authentication
  ) {
    UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
    Long senderId = user.getId();
    String content = body.get("content");

    ChatMessage saved = dialogService.sendMessage(
      dialogId,
      senderId,
      content,
      user.isClient()
    );
    ChatMessageDTO chatMessageDto = new ChatMessageDTO(
      saved.getId(),
      saved.getContent(),
      saved.getTimestamp(),
      saved.getDialog().getId(),
      user.getUsername(),
      saved.getType()
    );

    messagingTemplate.convertAndSend(
      "/topic/dialog/" + dialogId,
      chatMessageDto
    );
    return ResponseEntity.ok(chatMessageDto);
  }

  /**
   * Ferme un dialogue existant.
   *
   * @param dialogId ID du salon à fermer.
   * @param authentication Contexte d'authentification Spring Security.
   * @return ResponseEntity sans contenu.
   */
  @Operation(
    summary = "Fermer un dialogue",
    description = "Change le statut du dialogue en CLOSED et notifie les participants via WebSocket."
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "200",
        description = "Dialogue fermé avec succès"
      ),
      @ApiResponse(responseCode = "401", description = "Non authentifié"),
      @ApiResponse(responseCode = "404", description = "Dialogue non trouvé"),
    }
  )
  @PostMapping("/{dialogId}/close")
  public ResponseEntity<Void> closeDialog(
    @Parameter(
      description = "ID du dialogue",
      required = true
    ) @PathVariable Long dialogId,
    Authentication authentication
  ) {
    dialogService.closeDialog(dialogId);
    // Notification WebSocket de fermeture
    messagingTemplate.convertAndSend(
      "/topic/dialog/" + dialogId,
      Map.of("type", "CLOSE", "dialogId", dialogId)
    );
    return ResponseEntity.ok().build();
  }
}
