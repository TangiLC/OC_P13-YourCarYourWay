package com.ycyw.poc_chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la requête d'inscription (/auth/register).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

  @NotBlank
  @Email
  @Schema(
    description = "Email de l'utilisateur",
    example = "new@user.com",
    required = true
  )
  private String email;

  @NotBlank
  @Schema(
    description = "Mot de passe en clair",
    example = "Secret-1!",
    required = true
  )
  private String password;

  @NotBlank
  @Schema(
    description = "Rôle de l'utilisateur (USER, AGENT, ADMIN)",
    example = "USER",
    required = true
  )
  private String role;
}
