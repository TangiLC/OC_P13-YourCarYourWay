package com.ycyw.poc_chat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ycyw.poc_chat.model.UserCredential;

/**
 * Repository JPA pour les UserCredential.
 */
@Repository
public interface UserCredentialRepository
  extends JpaRepository<UserCredential, Long> {
  /**
   * Recherche un credential par email.
   * @param email l'email de l'utilisateur
   * @return l'entité UserCredential si trouvée
   */
  Optional<UserCredential> findByEmail(String email);

  /**
   * Vérifie si un credential existe pour l'email donné.
   * @param email l'email à tester
   * @return true si un utilisateur existe déjà avec cet email
   */
  boolean existsByEmail(String email);
}
