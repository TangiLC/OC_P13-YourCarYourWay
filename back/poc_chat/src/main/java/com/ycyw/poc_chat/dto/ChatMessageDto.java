package com.ycyw.poc_chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

import com.ycyw.poc_chat.model.MessageType;

import lombok.*;

/**
 * DTO utilisé pour l’échange de messages via WebSocket.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Message de chat échangé via WebSocket")
public class ChatMessageDTO {

  @Schema(description = "Identifiant du message")
  private Long id;

  @Schema(description = "Contenu du message")
  private String content;

  @Schema(description = "Horodatage du message")
  private LocalDateTime timestamp;

  @Schema(description = "Identifiant du dialogue auquel ce message appartient")
  private Long dialogId;

  @Schema(description = "Adresse email ou identifiant de l'expéditeur")
  private String sender;

  @Schema(description = "Type de message : CHAT, JOIN, LEAVE")
  private MessageType type;

}
