package com.ycyw.poc_chat.repository;

import com.ycyw.poc_chat.model.ChatMessage;
import com.ycyw.poc_chat.model.Dialog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA pour l'accès aux messages de chat.
 */
@Repository
public interface ChatMessageRepository
  extends JpaRepository<ChatMessage, Long> {
  /**
   * Récupère tous les messages d’un dialogue donné, triés par timestamp croissant.
   *
   * @param dialog le dialogue concerné
   * @return liste des messages
   */
  List<ChatMessage> findByDialogOrderByTimestampAsc(Dialog dialog);
}
