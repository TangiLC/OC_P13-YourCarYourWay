package com.ycyw.poc_chat.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entité représentant un message échangé dans un dialogue.
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MessageType type;

  @Column(nullable = false)
  private LocalDateTime timestamp;

  /**
   * Référence vers le dialogue parent.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dialog_id", nullable = false)
  private Dialog dialog;

  /**
   * Auteur du message.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private UserProfile sender;
}
