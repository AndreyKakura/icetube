package com.kakura.icetube.mapper;

import com.kakura.icetube.dto.RegistrationDto;
import com.kakura.icetube.dto.UserDto;
import com.kakura.icetube.model.Role;
import com.kakura.icetube.model.User;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        Collection<String> roles = user.getRoles().stream().map(Role::getName).toList();
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .surname(user.getSurname())
                .roles(roles)
                .build();
    }

    public User toModel(RegistrationDto registrationDto) {
        return User.builder()
                .username(registrationDto.getUsername())
                .name(registrationDto.getName())
                .surname(registrationDto.getSurname())
                .build();
    }
}
