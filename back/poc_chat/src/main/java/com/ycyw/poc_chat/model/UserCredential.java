package com.ycyw.poc_chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente les informations d'authentification d'un utilisateur.
 */
@Entity
@Table(name = "user_credentials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredential {

  /**
   * Identifiant unique, auto-incrémenté.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Email utilisé comme identifiant de connexion.
   */
  @Column(nullable = false, unique = true)
  private String email;

  /**
   * Mot de passe haché (BCrypt).
   */
  @Column(nullable = false)
  private String password;

  /**
   * Rôle de l'utilisateur (USER, AGENT, ADMIN).
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;
}
