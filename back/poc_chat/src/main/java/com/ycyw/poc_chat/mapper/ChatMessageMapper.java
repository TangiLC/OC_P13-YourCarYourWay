package com.ycyw.poc_chat.mapper;

import com.ycyw.poc_chat.dto.ChatMessageDto;
import com.ycyw.poc_chat.model.ChatMessage;
import com.ycyw.poc_chat.model.Dialog;
import com.ycyw.poc_chat.model.UserProfile;
import org.mapstruct.*;

/**
 * MapStruct mapper pour convertir entre entité ChatMessage et DTO.
 */
@Mapper(componentModel = "spring")
public interface ChatMessageMapper {
  @Mapping(source = "dialog.id", target = "dialogId")
  @Mapping(source = "sender.id", target = "sender")
  ChatMessageDto toDto(ChatMessage entity);

  /**
   * Convertit un DTO vers une entité.
   * Le `Dialog` et `UserProfile` doivent être injectés manuellement.
   */
  @Mapping(target = "dialog", ignore = true)
  @Mapping(target = "sender", ignore = true)
  ChatMessage toEntity(ChatMessageDto dto);

  /**
   * Enrichit une entité avec son dialogue et son expéditeur.
   */
  @AfterMapping
  default void linkEntities(
    ChatMessageDto dto,
    @MappingTarget ChatMessage message,
    @Context Dialog dialog,
    @Context UserProfile sender
  ) {
    message.setDialog(dialog);
    message.setSender(sender);
  }
}
