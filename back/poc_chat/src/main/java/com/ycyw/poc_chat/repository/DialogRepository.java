package com.ycyw.poc_chat.repository;

import com.ycyw.poc_chat.model.Dialog;
import com.ycyw.poc_chat.model.DialogStatus;
import com.ycyw.poc_chat.model.UserProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA pour l'accès aux dialogues.
 */
@Repository
public interface DialogRepository extends JpaRepository<Dialog, Long> {
  /**
   * Recherche le premier dialogue actif (OPEN) associé à un utilisateur donné.
   *
   * @param profile profil de l'utilisateur
   * @param status statut du dialogue (ex : OPEN)
   * @return un dialogue correspondant, s’il existe
   */
  Optional<Dialog> findFirstByParticipantsContainingAndStatus(
    UserProfile profile,
    DialogStatus status
  );

  /**
   * Récupère tous les dialogues auxquels un utilisateur a participé, triés par date de création.
   *
   * @param profile profil de l'utilisateur
   * @return liste des dialogues
   */
  List<Dialog> findByParticipantsContainingOrderByCreatedAtDesc(
    UserProfile profile
  );

  /**
   * Récupère tous les dialogues avec un statut donné (par exemple tous les dialogues ouverts).
   *
   * @param status statut des dialogues (OPEN, PENDING, CLOSED)
   * @return liste des dialogues concernés
   */
  List<Dialog> findByStatus(DialogStatus status);

  /**
   * Vérifie si un utilisateur a déjà participé à un dialogue donné.
   *
   * @param id identifiant du dialogue
   * @param profile utilisateur concerné
   * @return true si l'utilisateur est lié au dialogue
   */
  boolean existsByIdAndParticipantsContaining(Long id, UserProfile profile);
}
