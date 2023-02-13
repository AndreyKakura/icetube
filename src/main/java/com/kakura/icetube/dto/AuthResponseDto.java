package com.kakura.icetube.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private String accessToken;

    private String refreshToken;

    private Long id;

    private String username;

    private String name;

    private String surname;

    private List<String> roles;
}