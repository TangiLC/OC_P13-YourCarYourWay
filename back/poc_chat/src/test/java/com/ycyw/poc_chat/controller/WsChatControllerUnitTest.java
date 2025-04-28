package com.ycyw.poc_chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ycyw.poc_chat.dto.ChatMessageDTO;
import com.ycyw.poc_chat.lifecycle.DialogLifecycleManager;
import com.ycyw.poc_chat.model.ChatMessage;
import com.ycyw.poc_chat.model.Dialog;
import com.ycyw.poc_chat.model.MessageType;
import com.ycyw.poc_chat.model.UserProfile;
import com.ycyw.poc_chat.repository.UserProfileRepository;
import com.ycyw.poc_chat.security.UserPrincipal;
import com.ycyw.poc_chat.service.DialogService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WSChatControllerUnitTest {

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private DialogService dialogService;

  @Mock
  private UserProfileRepository userProfileRepository;

  @Mock
  private DialogLifecycleManager lifecycleManager;

  @Mock
  private SimpMessageHeaderAccessor headerAccessor;

  @Mock
  private Authentication authentication;

  @InjectMocks
  private WSChatController controller;

  private UserPrincipal userPrincipal;
  private UserProfile profile;
  private Dialog dialog;
  private ChatMessage savedMessage;

  @BeforeEach
  void setUp() {
    List<org.springframework.security.core.GrantedAuthority> auths = Collections.singletonList(
      new org.springframework.security.core.authority.SimpleGrantedAuthority(
        "ROLE_USER"
      )
    );
    userPrincipal = new UserPrincipal(1L, "testuser", "secret", auths);
    profile = new UserProfile();
    profile.setId(42L);
    profile.setFirstName("Alice");
    dialog = new Dialog();
    dialog.setId(7L);
    dialog.setTopic("General");
    savedMessage = new ChatMessage();
    savedMessage.setId(99L);
    savedMessage.setDialog(dialog);

    given(headerAccessor.getUser()).willReturn(authentication);
    given(authentication.getPrincipal()).willReturn(userPrincipal);
    given(userProfileRepository.findByUserCredentialId(1L)).willReturn(profile);
  }

  @Test
  @DisplayName("createDialog should invoke service and send to user queue")
  void createDialog_shouldCreateAndNotify() {
    given(dialogService.createDialog("Topic X", 1L)).willReturn(dialog);

    controller.createDialog("Topic X", headerAccessor);

    verify(dialogService).createDialog("Topic X", 1L);
    Map<String, Object> payload = Map.of("id", 7L, "topic", "General");
    verify(messagingTemplate)
      .convertAndSendToUser(
        eq("testuser"),
        eq("/queue/dialog-created"),
        eq(payload)
      );
  }

  @Test
  @DisplayName(
    "sendUserMessage with valid dialog should save, notify and lifecycle"
  )
  void sendUserMessage_validDialog_shouldProcessAndSend() {
    ChatMessageDTO messageDTO = new ChatMessageDTO();
    messageDTO.setDialogId(7L);
    messageDTO.setContent("Hello");

    given(dialogService.sendMessage(7L, 1L, "Hello", true))
      .willReturn(savedMessage);

    controller.sendUserMessage(messageDTO, headerAccessor);

    // ID set
    assertThat(messageDTO.getId()).isEqualTo(99L);
    // Type and sender populated
    assertThat(messageDTO.getType()).isEqualTo(MessageType.CHAT);
    assertThat(messageDTO.getSender()).isEqualTo("42");

    String destination = "/topic/dialog/7";
    verify(dialogService).sendMessage(7L, 1L, "Hello", true);
    verify(lifecycleManager).messageSent(7L);
    verify(messagingTemplate).convertAndSend(destination, messageDTO);
  }

  @Test
  @DisplayName("addUser should prepare JOIN message and notify and lifecycle")
  void addUser_shouldMarkJoinAndNotify() {
    ChatMessageDTO messageDTO = new ChatMessageDTO();
    messageDTO.setDialogId(7L);

    controller.addUser(messageDTO, headerAccessor);

    assertThat(messageDTO.getType()).isEqualTo(MessageType.JOIN);
    assertThat(messageDTO.getSender()).isEqualTo("Alice");
    String destination = "/topic/dialog/7";
    verify(messagingTemplate).convertAndSend(destination, messageDTO);
    verify(lifecycleManager).userJoined(7L, "testuser");
  }

  @Test
  @DisplayName(
    "disconnectUser should close and send LEAVE message and lifecycle"
  )
  void disconnectUser_shouldCloseDialogAndNotifyAndLifecycle() {
    ChatMessageDTO messageDTO = new ChatMessageDTO();
    messageDTO.setDialogId(7L);

    controller.disconnectUser(messageDTO, headerAccessor);

    assertThat(messageDTO.getType()).isEqualTo(MessageType.LEAVE);
    assertThat(messageDTO.getSender()).isEqualTo("Alice");
    verify(dialogService).closeDialog(7L);
    String destination = "/topic/dialog/7";
    verify(messagingTemplate).convertAndSend(destination, messageDTO);
    verify(lifecycleManager).userLeft(7L, "testuser");
  }
}
