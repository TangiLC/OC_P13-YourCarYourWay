package com.ycyw.poc_chat.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

/**
 * Entité représentant un dialogue entre utilisateurs, regroupant des messages.
 */
@Entity
@Table(name = "dialogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dialog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 255)
  private String topic;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DialogStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  /**
   * Participants (utilisateur, agent...) liés au dialogue.
   */
  @ManyToMany
  @JoinTable(
    name = "rel_user_dialog",
    joinColumns = @JoinColumn(name = "dialog_id"),
    inverseJoinColumns = @JoinColumn(name = "user_profile_id")
  )
  @Builder.Default
  private Set<UserProfile> participants = new HashSet<>();

  /**
   * Liste des messages associés à ce dialogue.
   */
  @OneToMany(
    mappedBy = "dialog",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  @Builder.Default
  private Set<ChatMessage> messages = new HashSet<>();

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
  }

  public void addMessage(ChatMessage message) {
    messages.add(message);
    message.setDialog(this);
  }
}
