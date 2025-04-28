package com.ycyw.poc_chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

import com.ycyw.poc_chat.mapper.DialogMapper;
import com.ycyw.poc_chat.model.ChatMessage;
import com.ycyw.poc_chat.model.Dialog;
import com.ycyw.poc_chat.model.DialogStatus;
import com.ycyw.poc_chat.model.MessageType;
import com.ycyw.poc_chat.model.UserProfile;
import com.ycyw.poc_chat.repository.ChatMessageRepository;
import com.ycyw.poc_chat.repository.DialogRepository;
import com.ycyw.poc_chat.repository.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DialogServiceUnitTest {

  @Mock
  private DialogRepository dialogRepository;

  @Mock
  private UserProfileRepository userProfileRepository;

  @Mock
  private ChatMessageRepository messageRepository;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @InjectMocks
  private DialogService dialogService;

  @Nested
  @DisplayName("createDialog")
  class CreateDialogTests {

    @Test
    @DisplayName("given topic, should append timestamp and save dialog")
    void givenTopic_shouldCreateWithTopicAndStatusPending() {
      // Given
      String topic = "MyTopic";
      // capture the saved dialog
      ArgumentCaptor<Dialog> captor = ArgumentCaptor.forClass(Dialog.class);
      given(dialogRepository.save(any(Dialog.class)))
        .willAnswer(inv -> inv.getArgument(0));

      // When
      Dialog result = dialogService.createDialog(topic, 123L);

      // Then
      then(dialogRepository).should().save(captor.capture());
      Dialog saved = captor.getValue();
      assertThat(saved.getTopic()).startsWith(topic + ".@");
      assertThat(saved.getStatus()).isEqualTo(DialogStatus.PENDING);
      assertThat(saved.getParticipants())
        .extracting(UserProfile::getId)
        .contains(123L);
      assertThat(result).isEqualTo(saved);
    }

    @Test
    @DisplayName("given null or blank topic, should generate default topic")
    void givenNullTopic_shouldGenerateDefaultTopic() {
      // Given
      String nullTopic = null;
      given(dialogRepository.save(any(Dialog.class)))
        .willAnswer(inv -> inv.getArgument(0));

      // When
      Dialog result = dialogService.createDialog(nullTopic, 7L);

      // Then
      assertThat(result.getTopic()).contains("Chat_");
      assertThat(result.getTopic()).contains(".@");
      assertThat(result.getStatus()).isEqualTo(DialogStatus.PENDING);
      assertThat(result.getParticipants())
        .extracting(UserProfile::getId)
        .contains(7L);
    }
  }

  @Nested
  @DisplayName("sendMessage")
  class SendMessageTests {

    private Dialog dialog;
    private UserProfile profile;

    @BeforeEach
    void init() {
      profile = UserProfile.builder().id(42L).build();
      dialog =
        Dialog
          .builder()
          .id(9L)
          .status(DialogStatus.PENDING)
          .participants(new HashSet<>())
          .build();
      given(dialogRepository.findById(9L)).willReturn(Optional.of(dialog));
    }

    @Test
    @DisplayName(
      "when client sends message and dialog closed, reopen and add participant"
    )
    void clientWhenClosed_shouldReopenAndAddParticipant() {
      dialog.setStatus(DialogStatus.CLOSED);
      given(messageRepository.save(any(ChatMessage.class)))
        .willAnswer(inv -> inv.getArgument(0));

      ChatMessage msg = dialogService.sendMessage(9L, 42L, "Hi", true);

      assertThat(dialog.getStatus()).isEqualTo(DialogStatus.PENDING);
      assertThat(dialog.getClosedAt()).isNull();
      assertThat(dialog.getParticipants())
        .extracting(UserProfile::getId)
        .contains(42L);
      assertThat(msg.getContent()).isEqualTo("Hi");
      assertThat(msg.getType()).isEqualTo(MessageType.CHAT);
      then(messageRepository).should().save(any(ChatMessage.class));
    }

    @Test
    @DisplayName(
      "when agent sends message, should open dialog if pending and add participant"
    )
    void agentWhenPending_shouldOpenAndAddParticipant() {
      // agent = isClient false
      given(userProfileRepository.getReferenceById(100L))
        .willReturn(UserProfile.builder().id(100L).build());
      given(messageRepository.save(any(ChatMessage.class)))
        .willAnswer(inv -> inv.getArgument(0));

      ChatMessage msg = dialogService.sendMessage(
        9L,
        100L,
        "HelloAgent",
        false
      );

      assertThat(dialog.getStatus()).isEqualTo(DialogStatus.OPEN);
      assertThat(dialog.getParticipants())
        .extracting(UserProfile::getId)
        .contains(100L);
      assertThat(msg.getSender().getId()).isEqualTo(100L);
      then(messageRepository).should().save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("when dialog not found, should throw")
    void whenNotFound_shouldThrow() {
      given(dialogRepository.findById(99L)).willReturn(Optional.empty());
      assertThatThrownBy(() -> dialogService.sendMessage(99L, 1L, "", false))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Dialog not found");
    }
  }

  @Nested
  @DisplayName("closeDialog")
  class CloseDialogTests {

    @Test
    @DisplayName("when open, should close and save")
    void whenOpen_shouldCloseAndSave() {
      Dialog d = Dialog.builder().id(5L).status(DialogStatus.OPEN).build();
      given(dialogRepository.findById(5L)).willReturn(Optional.of(d));
      given(dialogRepository.save(any(Dialog.class)))
        .willAnswer(inv -> inv.getArgument(0));

      Dialog result = dialogService.closeDialog(5L);

      assertThat(result.getStatus()).isEqualTo(DialogStatus.CLOSED);
      assertThat(result.getClosedAt()).isNotNull();
      then(dialogRepository).should().save(d);
    }

    @Test
    @DisplayName("when already closed, should throw")
    void whenAlreadyClosed_shouldThrow() {
      Dialog d = Dialog.builder().id(6L).status(DialogStatus.CLOSED).build();
      given(dialogRepository.findById(6L)).willReturn(Optional.of(d));
      assertThatThrownBy(() -> dialogService.closeDialog(6L))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Dialog already closed");
    }

    @Test
    @DisplayName("when not found, should throw")
    void whenNotFound_shouldThrow() {
      given(dialogRepository.findById(8L)).willReturn(Optional.empty());
      assertThatThrownBy(() -> dialogService.closeDialog(8L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Dialog not found");
    }
  }

  @Nested
  @DisplayName("inviteUser")
  class InviteUserTests {

    @Test
    @DisplayName("should add participant and send invite")
    void shouldAddAndNotify() {
      Dialog d = Dialog.builder().id(11L).participants(new HashSet<>()).build();
      given(dialogRepository.findById(11L)).willReturn(Optional.of(d));
      UserProfile u = UserProfile.builder().id(22L).build();
      given(userProfileRepository.findById(22L)).willReturn(Optional.of(u));

      dialogService.inviteUser(11L, 22L);

      assertThat(d.getParticipants()).contains(u);
      then(dialogRepository).should().save(d);
      then(messagingTemplate)
        .should()
        .convertAndSend(eq("/topic/dialog/11/invites"), any(Map.class));
    }

    @Test
    @DisplayName("when dialog not found, should throw EntityNotFoundException")
    void whenDialogNotFound_shouldThrow() {
      given(dialogRepository.findById(33L)).willReturn(Optional.empty());
      assertThatThrownBy(() -> dialogService.inviteUser(33L, 1L))
        .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("when user not found, should throw EntityNotFoundException")
    void whenUserNotFound_shouldThrow() {
      Dialog d = Dialog.builder().id(44L).participants(new HashSet<>()).build();
      given(dialogRepository.findById(44L)).willReturn(Optional.of(d));
      given(userProfileRepository.findById(55L)).willReturn(Optional.empty());
      assertThatThrownBy(() -> dialogService.inviteUser(44L, 55L))
        .isInstanceOf(EntityNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("markMessagesAsRead")
  class MarkMessagesAsReadTests {

    @Test
    @DisplayName("should mark unread and notify")
    void shouldMarkReadAndNotify() {
      ChatMessage m1 = ChatMessage.builder().isRead(false).build();
      ChatMessage m2 = ChatMessage.builder().isRead(false).build();
      given(
        messageRepository.findByDialog_IdAndSender_IdNotAndIsReadFalse(7L, 2L)
      )
        .willReturn(Arrays.asList(m1, m2));

      dialogService.markMessagesAsRead(7L, 2L);

      assertThat(m1.getIsRead()).isTrue();
      assertThat(m2.getIsRead()).isTrue();
      then(messageRepository).should().saveAll(Arrays.asList(m1, m2));
      then(messagingTemplate)
        .should()
        .convertAndSend(eq("/topic/dialog/7/read"), any(Map.class));
    }
  }
}
