package com.ycyw.poc_chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la requête de connexion (/auth/login).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

  @NotBlank
  @Email
  @Schema(
    description = "Email de l'utilisateur",
    example = "user@test.com",
    required = true
  )
  private String email;

  @NotBlank
  @Schema(
    description = "Mot de passe en clair",
    example = "Test-1!",
    required = true
  )
  private String password;
}
