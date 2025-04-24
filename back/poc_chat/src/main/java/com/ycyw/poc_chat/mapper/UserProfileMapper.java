package com.ycyw.poc_chat.mapper;

import com.ycyw.poc_chat.dto.UserProfileRequest;
import com.ycyw.poc_chat.dto.UserProfileDTO;
import com.ycyw.poc_chat.model.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
  UserProfileDTO toResponse(UserProfile entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userCredential", ignore = true)
  UserProfile toEntity(UserProfileRequest request);
}
