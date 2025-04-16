package com.ycyw.poc_chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse d'authentification contenant le JWT.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

  @Schema(description = "Token JWT signé", example = "eyJhbGciOiJIUzUxMiJ9...")
  private String token;

  @Schema(description = "ID de l'utilisateur", example = "1")
  private Long id;

  @Schema(description = "Email de l'utilisateur", example = "user@test.com")
  private String email;

  @Schema(description = "Rôle de l'utilisateur", example = "USER")
  private String role;
}
