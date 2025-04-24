package com.ycyw.poc_chat.dto;

import com.ycyw.poc_chat.model.ProfileType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Requête de création de profil utilisateur")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileRequest {

  @Schema(description = "Prénom de l'utilisateur", example = "John")
  private String firstName;

  @Schema(description = "Nom de l'utilisateur", example = "Doe")
  private String lastName;

  @Schema(description = "Entreprise associée (si applicable)", example = "YCYW")
  private String company;

  @Schema(description = "Type de profil utilisateur", example = "INDIVIDUAL")
  private ProfileType type;
}
