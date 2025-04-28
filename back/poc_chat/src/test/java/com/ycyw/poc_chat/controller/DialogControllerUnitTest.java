package com.ycyw.poc_chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import com.ycyw.poc_chat.dto.DialogDTO;
import com.ycyw.poc_chat.mapper.DialogMapper;
import com.ycyw.poc_chat.model.Dialog;
import com.ycyw.poc_chat.model.DialogStatus;
import com.ycyw.poc_chat.repository.DialogRepository;
import com.ycyw.poc_chat.service.DialogService;
import jakarta.persistence.EntityNotFoundException;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class DialogControllerUnitTest {

  @Mock
  private DialogRepository repo;

  @Mock
  private DialogService service;

  @Mock
  private DialogMapper mapper;

  @InjectMocks
  private DialogController controller;

  private Dialog dialog;
  private DialogDTO dto;

  @BeforeEach
  void setUp() {
    dialog = new Dialog();
    dialog.setId(1L);
    dialog.setStatus(DialogStatus.OPEN);
    dto = new DialogDTO();
    dto.setId(1L);
    dto.setStatus(DialogStatus.OPEN);
  }

  @Test
  @DisplayName("Should return a list of all dialogs with HTTP 200")
  void getAllDialogs_shouldReturnListOfDTOs() {
    
    Set<Dialog> set = Set.of(dialog);
    given(repo.findAllWithMessagesAndSenders()).willReturn(set);
    given(mapper.toDialogDTO(dialog)).willReturn(dto);
    
    ResponseEntity<List<DialogDTO>> resp = controller.getAllDialogs();

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).hasSize(1);
    assertThat(resp.getBody()).containsExactly(dto);
    then(repo).should().findAllWithMessagesAndSenders();
    then(mapper).should().toDialogDTO(dialog);
  }

  @Test
  @DisplayName("Should return empty list when no dialogs exist")
  void getAllDialogs_whenEmpty_shouldReturnEmptyList() {
    
    given(repo.findAllWithMessagesAndSenders())
      .willReturn(Collections.emptySet());

    ResponseEntity<List<DialogDTO>> resp = controller.getAllDialogs();

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(DialogStatus.class)
  @DisplayName("Should filter dialogs by all possible status values")
  void getDialogsByStatus_shouldFilterByStatus(DialogStatus status) {
    
    Dialog statusDialog = new Dialog();
    statusDialog.setId(1L);
    statusDialog.setStatus(status);

    DialogDTO statusDto = new DialogDTO();
    statusDto.setId(1L);
    statusDto.setStatus(status);

    List<Dialog> list = List.of(statusDialog);
    given(repo.findByStatus(status)).willReturn(list);
    given(mapper.toDialogDTO(statusDialog)).willReturn(statusDto);

    List<DialogDTO> result = controller.getDialogsByStatus(status);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(status);
    then(repo).should().findByStatus(status);
  }

  @Test
  @DisplayName("Should return empty list when no dialogs with specified status")
  void getDialogsByStatus_whenNoMatches_shouldReturnEmptyList() {
    
    given(repo.findByStatus(any(DialogStatus.class)))
      .willReturn(Collections.emptyList());

    List<DialogDTO> result = controller.getDialogsByStatus(DialogStatus.CLOSED);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should return dialog by ID when found")
  void getDialogById_found() {
    
    given(repo.findByIdWithMessagesAndSenders(1L))
      .willReturn(Optional.of(dialog));
    given(mapper.toDialogDTO(dialog)).willReturn(dto);

    ResponseEntity<DialogDTO> resp = controller.getDialogById(1L);

    
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).isEqualTo(dto);
    then(mapper).should().toDialogDTO(dialog);
  }

  @ParameterizedTest
  @ValueSource(longs = { 2L, 999L, -1L })
  @DisplayName("Should return 404 when dialog not found by different IDs")
  void getDialogById_notFound(long id) {
    
    given(repo.findByIdWithMessagesAndSenders(id)).willReturn(Optional.empty());

    
    ResponseEntity<DialogDTO> resp = controller.getDialogById(id);

    
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(resp.getBody()).isNull();
    then(mapper).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("Should return dialogs by sender ID")
  void getDialogsBySender_shouldReturnDialogs() {
    
    long senderId = 42L;
    Set<Dialog> dialogs = Set.of(dialog);
    given(repo.findDistinctByParticipantsIdWithMessagesAndSenders(senderId))
      .willReturn(dialogs);
    given(mapper.toDialogDTO(dialog)).willReturn(dto);

    Set<DialogDTO> result = controller.getDialogsBySender(senderId);

    assertThat(result).hasSize(1);
    assertThat(result).contains(dto);
    then(repo)
      .should()
      .findDistinctByParticipantsIdWithMessagesAndSenders(senderId);
    then(mapper).should().toDialogDTO(dialog);
  }

  @Test
  @DisplayName("Should return empty set when no dialogs found for sender")
  void getDialogsBySender_whenNoMatches_shouldReturnEmptySet() {
    
    long senderId = 42L;
    given(repo.findDistinctByParticipantsIdWithMessagesAndSenders(senderId))
      .willReturn(Collections.emptySet());

    Set<DialogDTO> result = controller.getDialogsBySender(senderId);

    assertThat(result).isEmpty();
    then(mapper).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("Should successfully invite user to dialog")
  void inviteUserToDialog_success() {
    
    willDoNothing().given(service).inviteUser(1L, 42L);

    ResponseEntity<Void> resp = controller.inviteUserToDialog(1L, 42L);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    then(service).should().inviteUser(1L, 42L);
  }

  @ParameterizedTest
  @MethodSource("invalidDialogUserCombinations")
  @DisplayName("Should return 404 for invalid dialog/user combinations")
  void inviteUserToDialog_notFound(long dialogId, long userId) {
    
    willThrow(EntityNotFoundException.class)
      .given(service)
      .inviteUser(dialogId, userId);

    ResponseEntity<Void> resp = controller.inviteUserToDialog(dialogId, userId);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    then(service).should().inviteUser(dialogId, userId);
  }

  static Stream<Arguments> invalidDialogUserCombinations() {
    return Stream.of(
      Arguments.of(999L, 42L), // Non-existent dialog
      Arguments.of(1L, 999L), // Non-existent user
      Arguments.of(-1L, 42L), // Invalid dialog ID
      Arguments.of(1L, -1L) // Invalid user ID
    );
  }

  @Test
  @DisplayName("Should mark messages as read successfully")
  void markMessagesAsRead_success() {
    
    willDoNothing().given(service).markMessagesAsRead(1L, 99L);

    ResponseEntity<Void> resp = controller.markMessagesAsRead(1L, 99L);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    then(service).should().markMessagesAsRead(1L, 99L);
  }

  @ParameterizedTest
  @MethodSource("invalidMarkAsReadCombinations")
  @DisplayName("Should return 404 for invalid markAsRead parameters")
  void markMessagesAsRead_notFound(long dialogId, long senderId) {
    
    willThrow(EntityNotFoundException.class)
      .given(service)
      .markMessagesAsRead(dialogId, senderId);

    ResponseEntity<Void> resp = controller.markMessagesAsRead(
      dialogId,
      senderId
    );

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    then(service).should().markMessagesAsRead(dialogId, senderId);
  }

  static Stream<Arguments> invalidMarkAsReadCombinations() {
    return Stream.of(
      Arguments.of(999L, 99L), // Non-existent dialog
      Arguments.of(1L, 999L), // Non-existent sender
      Arguments.of(-1L, 99L), // Invalid dialog ID
      Arguments.of(1L, -1L) // Invalid sender ID
    );
  }

  @Test
  @DisplayName("Should handle empty dialogs by sender")
  void getDialogsBySender_whenEmpty() {
    
    long senderId = 77L;
    given(repo.findDistinctByParticipantsIdWithMessagesAndSenders(senderId))
      .willReturn(Collections.emptySet());

    Set<DialogDTO> result = controller.getDialogsBySender(senderId);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should handle multiple dialogs in results")
  void getAllDialogs_withMultipleDialogs() {
    
    Dialog dialog1 = new Dialog();
    dialog1.setId(1L);
    dialog1.setStatus(DialogStatus.OPEN);

    Dialog dialog2 = new Dialog();
    dialog2.setId(2L);
    dialog2.setStatus(DialogStatus.PENDING);

    DialogDTO dto1 = new DialogDTO();
    dto1.setId(1L);
    dto1.setStatus(DialogStatus.OPEN);

    DialogDTO dto2 = new DialogDTO();
    dto2.setId(2L);
    dto2.setStatus(DialogStatus.PENDING);

    Set<Dialog> dialogs = new HashSet<>(Arrays.asList(dialog1, dialog2));

    given(repo.findAllWithMessagesAndSenders()).willReturn(dialogs);
    given(mapper.toDialogDTO(dialog1)).willReturn(dto1);
    given(mapper.toDialogDTO(dialog2)).willReturn(dto2);

    
    ResponseEntity<List<DialogDTO>> response = controller.getAllDialogs();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
    assertThat(response.getBody()).containsExactlyInAnyOrder(dto1, dto2);
  }
}
