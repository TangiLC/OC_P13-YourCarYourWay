package com.ycyw.poc_chat.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "dialogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Dialog {

  @Id
  @EqualsAndHashCode.Include
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 25)
  private String topic;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DialogStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  @Column(name = "last_activity_at", nullable = false)
  private LocalDateTime lastActivityAt;

  @ManyToMany
  @JoinTable(
    name = "rel_user_dialog",
    joinColumns = @JoinColumn(name = "dialog_id"),
    inverseJoinColumns = @JoinColumn(name = "user_profile_id")
  )
  @JsonManagedReference
  @Builder.Default
  private Set<UserProfile> participants = new HashSet<>();

  @OneToMany(
    mappedBy = "dialog",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  @Builder.Default
  @JsonManagedReference
  private Set<ChatMessage> messages = new HashSet<>();

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.lastActivityAt = LocalDateTime.now();
  }

  @PreUpdate
  public void onUpdate() {
    this.lastActivityAt = LocalDateTime.now();
  }

  public void addMessage(ChatMessage message) {
    messages.add(message);
    message.setDialog(this);
  }
}
