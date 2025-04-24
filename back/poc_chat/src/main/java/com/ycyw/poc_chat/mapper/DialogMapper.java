package com.ycyw.poc_chat.mapper;

import com.ycyw.poc_chat.dto.DialogDTO;
import com.ycyw.poc_chat.model.Dialog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
  componentModel = "spring",
  uses = { ChatMessageMapper.class, UserProfileMapper.class }
)
public interface DialogMapper {
  @Mapping(target = "participants", source = "participants")
  @Mapping(target = "messages", source = "messages")
  DialogDTO toDialogDTO(Dialog dialog);
}
