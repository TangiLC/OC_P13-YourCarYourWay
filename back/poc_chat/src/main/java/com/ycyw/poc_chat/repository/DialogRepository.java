package com.ycyw.poc_chat.repository;

import com.ycyw.poc_chat.model.Dialog;
import com.ycyw.poc_chat.model.DialogStatus;
import com.ycyw.poc_chat.model.UserProfile;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
   * Vérifie si un utilisateur a déjà participé à un dialogue donné.
   *
   * @param id identifiant du dialogue
   * @param profile utilisateur concerné
   * @return true si l'utilisateur est lié au dialogue
   */
  boolean existsByIdAndParticipantsContaining(Long id, UserProfile profile);

  /**
   * Récupère tous les dialogues avec un statut donné (OPEN, PENDING, CLOSED).
   *
   * @param status statut recherché
   * @return liste des Dialog correspondants
   */
  List<Dialog> findByStatus(DialogStatus status);

  /**
   * Récupère tous les dialogues avec un statut donné (OPEN, PENDING, CLOSED).
   *
   * @param status statut recherché
   * @return liste des Dialog correspondants
   */
  @Query(
    """
        SELECT DISTINCT d
        FROM Dialog d
        LEFT JOIN FETCH d.participants p
        LEFT JOIN FETCH d.messages m
        LEFT JOIN FETCH m.sender s
        WHERE d.status = :status
        """
  )
  List<Dialog> findByStatusWithMessagesAndSenders(
    @Param("status") DialogStatus status
  );

  /**
   * Recherche un dialogue par son ID, retourne messages et auteurs.
   *
   * @param id identifiant du dialogue
   * @return Optionnel contenant le Dialogue avec ses messages si trouvé
   */
  @Query(
    """
        SELECT DISTINCT d
        FROM Dialog d
        LEFT JOIN FETCH d.participants p
        LEFT JOIN FETCH d.messages m
        LEFT JOIN FETCH m.sender
        WHERE d.id = :id
        """
  )
  Optional<Dialog> findByIdWithMessagesAndSenders(@Param("id") Long id);

  /**
   * Récupère tous les dialogues avec messages postés par l'utilisateur {id}.
   *
   * @param senderId identifiant du sender
   * @return ensemble des Dialog distincts
   */
  Set<Dialog> findDistinctByParticipants_Id(Long senderId);

  /**
   * Récupère tous les dialogues avec messages postés par l'utilisateur {id}.
   *
   * @param senderId identifiant du sender
   * @return ensemble des Dialog distincts
   */
  @Query(
    """
        SELECT DISTINCT d
        FROM Dialog d
        JOIN d.participants p
        LEFT JOIN FETCH d.participants
        LEFT JOIN FETCH d.messages m
        LEFT JOIN FETCH m.sender s
        WHERE p.id = :id
        """
  )
  Set<Dialog> findDistinctByParticipantsIdWithMessagesAndSenders(
    @Param("id") Long id
  );

  /**
   * Récupère tous les dialogues avec leurs messages et senders.
   *
   * @return ensemble de tous les Dialog
   */
  @Query(
    """
        SELECT DISTINCT d
        FROM Dialog d
        LEFT JOIN FETCH d.participants p
        LEFT JOIN FETCH d.messages m
        LEFT JOIN FETCH m.sender
        """
  )
  Set<Dialog> findAllWithMessagesAndSenders();
}
