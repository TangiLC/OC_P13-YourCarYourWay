package com.ycyw.poc_chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO pour la réponse de création d'un salon de discussion.
 */
@Data
@AllArgsConstructor
@Schema(description = "Réponse de la création d'un dialogue")
public class DialogResponseDTO {

  @Schema(description = "Identifiant du dialogue")
  private Long id;

  @Schema(description = "Sujet du dialogue")
  private String topic;
}
