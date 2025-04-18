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

  @Column(length = 255)
  @EqualsAndHashCode.Exclude
  private String topic;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @EqualsAndHashCode.Exclude
  private DialogStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  @EqualsAndHashCode.Exclude
  private LocalDateTime createdAt;

  @Column(name = "closed_at")
  @EqualsAndHashCode.Exclude
  private LocalDateTime closedAt;

  @ManyToMany
  @JoinTable(
    name = "rel_user_dialog",
    joinColumns = @JoinColumn(name = "dialog_id"),
    inverseJoinColumns = @JoinColumn(name = "user_profile_id")
  )
  @JsonManagedReference
  @EqualsAndHashCode.Exclude
  private Set<UserProfile> participants = new HashSet<>();

  @OneToMany(
    mappedBy = "dialog",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  @Builder.Default
  @JsonManagedReference
  @EqualsAndHashCode.Exclude
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
