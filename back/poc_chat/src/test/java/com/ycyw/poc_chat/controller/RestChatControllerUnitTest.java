package com.ycyw.poc_chat.controller;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.ycyw.poc_chat.dto.ChatMessageDTO;
import com.ycyw.poc_chat.dto.DialogResponseDTO;
import com.ycyw.poc_chat.model.ChatMessage;
import com.ycyw.poc_chat.model.Dialog;
import com.ycyw.poc_chat.model.DialogStatus;
import com.ycyw.poc_chat.model.MessageType;
import com.ycyw.poc_chat.security.UserPrincipal;
import com.ycyw.poc_chat.service.DialogService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RestChatControllerUnitTest {

  @Mock
  private DialogService dialogService;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private Authentication authentication;

  @InjectMocks
  private RestChatController controller;

  private UserPrincipal userPrincipal;
  private Dialog dialog;
  private ChatMessage message;

  @BeforeEach
  void setUp() {
    // Setup user principal with a USER role
    List<GrantedAuthority> authorities = Collections.singletonList(
      new SimpleGrantedAuthority("ROLE_USER")
    );
    userPrincipal = new UserPrincipal(1L, "testuser", "password", authorities);
    given(authentication.getPrincipal()).willReturn(userPrincipal);

    // Setup dialog
    dialog = new Dialog();
    dialog.setId(1L);
    dialog.setTopic("Test Topic");
    dialog.setStatus(DialogStatus.OPEN);

    // Setup message
    message = new ChatMessage();
    message.setId(1L);
    message.setContent("Test message");
    message.setTimestamp(LocalDateTime.now());
    message.setDialog(dialog);
    message.setType(MessageType.CHAT);
  }

  @ParameterizedTest
  @ValueSource(
    strings = { "Test Topic", "Another Topic", "Important Discussion" }
  )
  @DisplayName("Should create dialog with provided topic")
  void createDialog_withTopic_shouldCreateAndReturn(String topic) {
    given(dialogService.createDialog(topic, 1L)).willReturn(dialog);

    ResponseEntity<DialogResponseDTO> response = controller.createDialog(
      topic,
      authentication
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo(dialog.getId());
    assertThat(response.getBody().getTopic()).isEqualTo(dialog.getTopic());

    then(messagingTemplate)
      .should()
      .convertAndSendToUser(
        eq("testuser"),
        eq("/queue/dialog-created"),
        any(DialogResponseDTO.class)
      );
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Should create dialog with null or empty topic")
  void createDialog_withNullOrEmptyTopic_shouldCreateWithDefaultTopic(
    String topic
  ) {
    given(dialogService.createDialog(topic, 1L)).willReturn(dialog);

    ResponseEntity<DialogResponseDTO> response = controller.createDialog(
      topic,
      authentication
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    then(dialogService).should().createDialog(topic, 1L);

    then(messagingTemplate)
      .should()
      .convertAndSendToUser(
        anyString(),
        anyString(),
        any(DialogResponseDTO.class)
      );
  }

  @Test
  @DisplayName("Should send message and notify via WebSocket")
  void sendMessage_shouldSaveAndNotify() {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("content", "Test message");

    given(
      dialogService.sendMessage(
        eq(1L),
        eq(1L),
        eq("Test message"),
        anyBoolean()
      )
    )
      .willReturn(message);

    ResponseEntity<ChatMessageDTO> response = controller.sendMessage(
      1L,
      requestBody,
      authentication
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent()).isEqualTo("Test message");
    assertThat(response.getBody().getDialogId()).isEqualTo(1L);

    then(messagingTemplate)
      .should()
      .convertAndSend(eq("/topic/dialog/1"), any(ChatMessageDTO.class));
  }

  @Test
  @DisplayName("Should send message as client user")
  void sendMessage_asClient_shouldSetClientFlag() {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("content", "Client message");

    given(
      dialogService.sendMessage(eq(1L), eq(1L), eq("Client message"), eq(true))
    )
      .willReturn(message);

    controller.sendMessage(1L, requestBody, authentication);

    then(dialogService).should().sendMessage(1L, 1L, "Client message", true);
  }

  @ParameterizedTest
  @MethodSource("messageContents")
  @DisplayName("Should handle different message contents")
  void sendMessage_withDifferentContents(String content) {
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("content", content);

    message.setContent(content);
    given(dialogService.sendMessage(eq(1L), eq(1L), eq(content), anyBoolean()))
      .willReturn(message);

    ResponseEntity<ChatMessageDTO> response = controller.sendMessage(
      1L,
      requestBody,
      authentication
    );

    assertThat(response.getBody().getContent()).isEqualTo(content);
    then(dialogService).should().sendMessage(1L, 1L, content, true);
  }

  static Stream<Arguments> messageContents() {
    return Stream.of(
      Arguments.of("Hello, world!"),
      Arguments.of("Special chars: !@#$%^&*()"),
      Arguments.of(""),
      Arguments.of(
        "Very long message that exceeds typical lengths and might cause issues if not handled properly. This message is intentionally long to test how the system handles longer content. It should be processed correctly without truncation or other problems."
      )
    );
  }

  @Test
  @DisplayName("Should close dialog and notify via WebSocket")
  void closeDialog_shouldCloseAndNotify() {
    ResponseEntity<Void> response = controller.closeDialog(1L, authentication);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    then(dialogService).should().closeDialog(1L);

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(
      Map.class
    );
    then(messagingTemplate)
      .should()
      .convertAndSend(eq("/topic/dialog/1"), captor.capture());

    Map<String, Object> sentMessage = captor.getValue();
    assertThat(sentMessage).containsEntry("type", "CLOSE");
    assertThat(sentMessage).containsEntry("dialogId", 1L);
  }

  @ParameterizedTest
  @ValueSource(longs = { 1L, 2L, 999L })
  @DisplayName("Should close dialogs with different IDs")
  void closeDialog_withDifferentIds_shouldCloseCorrectDialog(Long dialogId) {
    ResponseEntity<Void> response = controller.closeDialog(
      dialogId,
      authentication
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    then(dialogService).should().closeDialog(dialogId);
    then(messagingTemplate)
      .should()
      .convertAndSend(eq("/topic/dialog/" + dialogId), any(Map.class));
  }

  @Test
  @DisplayName("Should create multiple dialogs with unique IDs")
  void createMultipleDialogs_shouldCreateUniqueDialogs() {
    Dialog dialog1 = new Dialog();
    dialog1.setId(1L);
    dialog1.setTopic("Topic 1");

    Dialog dialog2 = new Dialog();
    dialog2.setId(2L);
    dialog2.setTopic("Topic 2");

    given(dialogService.createDialog("Topic 1", 1L)).willReturn(dialog1);
    given(dialogService.createDialog("Topic 2", 1L)).willReturn(dialog2);

    ResponseEntity<DialogResponseDTO> response1 = controller.createDialog(
      "Topic 1",
      authentication
    );
    ResponseEntity<DialogResponseDTO> response2 = controller.createDialog(
      "Topic 2",
      authentication
    );

    assertThat(response1.getBody().getId()).isEqualTo(1L);
    assertThat(response2.getBody().getId()).isEqualTo(2L);
    then(messagingTemplate)
      .should(times(2))
      .convertAndSendToUser(
        anyString(),
        anyString(),
        any(DialogResponseDTO.class)
      );
  }

  @Test
  @DisplayName("Should handle special characters in message content")
  void sendMessage_withSpecialChars_shouldHandleCorrectly() {
    String specialContent = "Special chars: !@#$%^&*()_+{}|:\"<>?~`-=[]\\;',./";
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("content", specialContent);

    message.setContent(specialContent);
    given(
      dialogService.sendMessage(
        anyLong(),
        anyLong(),
        eq(specialContent),
        anyBoolean()
      )
    )
      .willReturn(message);

    ResponseEntity<ChatMessageDTO> response = controller.sendMessage(
      1L,
      requestBody,
      authentication
    );

    assertThat(response.getBody().getContent()).isEqualTo(specialContent);
  }
}
