package com.kakura.icetube.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeNameAndSurnameDto {
    @NotBlank(message = "New name should not be blank")
    private String name;

    @NotBlank(message = "New surname should not be blank")
    private String surname;

    @NotBlank(message = "Password should not be blank")
    private String password;
}
