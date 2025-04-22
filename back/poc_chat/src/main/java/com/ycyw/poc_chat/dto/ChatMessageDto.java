package com.ycyw.poc_chat.dto;

import com.ycyw.poc_chat.model.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
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

  @Schema(description = "Identifiant de l'expéditeur")
  private String sender;

  @Schema(description = "Indique si le message a été lu")
  private Boolean isRead;

  @Schema(description = "Type de message : CHAT, JOIN, LEAVE")
  private MessageType type;
}
