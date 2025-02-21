package com.double2and9.auth_service.mapper;

import com.double2and9.auth_service.dto.response.UserResponse;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStrings")
    UserResponse toUserResponse(User user);

    @Named("rolesToStrings")
    default Set<String> rolesToStrings(Set<Role> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
} 