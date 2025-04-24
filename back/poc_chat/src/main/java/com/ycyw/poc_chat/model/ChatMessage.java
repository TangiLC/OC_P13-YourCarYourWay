package com.ycyw.poc_chat.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChatMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  @Column(columnDefinition = "TEXT", nullable = false)
  @EqualsAndHashCode.Include
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @EqualsAndHashCode.Include
  private MessageType type;

  @Column(nullable = false)
  @EqualsAndHashCode.Include
  private LocalDateTime timestamp;

  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
  @EqualsAndHashCode.Include
  @Builder.Default
  private Boolean isRead = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dialog_id", nullable = false)
  @JsonBackReference
  private Dialog dialog;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private UserProfile sender;
}
