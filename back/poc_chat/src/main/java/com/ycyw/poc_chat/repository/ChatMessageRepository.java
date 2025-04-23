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


  /**
   * Récupère tous les messages non lus d’un dialogue donné, excepté un sender donné.
   *
   * @param dialogId le dialogue concerné
   * @param senderId le dialogue concerné
   * @return liste des messages
   */
  List<ChatMessage> findByDialog_IdAndSender_IdNotAndIsReadFalse(Long dialogId, Long senderId);
}
