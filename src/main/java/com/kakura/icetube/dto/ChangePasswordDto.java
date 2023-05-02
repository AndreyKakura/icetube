package com.kakura.icetube.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordDto {
    @NotBlank(message = "Old password should not be blank")
    private String oldPassword;

    @NotBlank(message = "New password should not be blank")
    private String newPassword;
}
