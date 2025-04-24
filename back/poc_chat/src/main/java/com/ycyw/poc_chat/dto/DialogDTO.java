package com.ycyw.poc_chat.dto;

import com.ycyw.poc_chat.model.DialogStatus;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;

@Data
public class DialogDTO {

  private Long id;
  private String topic;
  private DialogStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime closedAt;
  private LocalDateTime lastActivityAt;
  private Set<UserProfileDTO> participants = new HashSet<>();
  private Set<ChatMessageDTO> messages = new HashSet<>();
}
