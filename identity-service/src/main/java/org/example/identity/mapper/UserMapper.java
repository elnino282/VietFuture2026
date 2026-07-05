package org.example.identity.mapper;

import org.example.identity.entity.User;
import org.example.identity.controller.InternalUserController.UserInternalDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    UserInternalDto toInternalDto(User user);
}
