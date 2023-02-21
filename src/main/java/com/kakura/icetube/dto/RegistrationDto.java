package com.kakura.icetube.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationDto {

    @NotEmpty(message = "Username should not be empty")
    @Length(min = 4, max = 30, message = "Length: 4-30")
    @Pattern(regexp = "[A-Za-z0-9]+", message = "Only latin letters and numbers")
    private String username;

    @NotEmpty(message = "Name should not be empty")
    @Length(min = 2, max = 30, message = "Length: 2-30")
    @Pattern(regexp = "[A-Za-zА-Яа-я]+", message = "Only latin and cyrillic letters")
    private String name;

    @NotEmpty(message = "Surname should not be empty")
    @Length(min = 2, max = 30, message = "Length: 2-30")
    @Pattern(regexp = "[A-Za-zА-Яа-я]+", message = "Only latin and cyrillic letters")
    private String surname;

    @NotEmpty(message = "Password should not be empty")
    @Length(min = 4, max = 30, message = "Length: 4-30")
    @Pattern(regexp = "\\S*", message = "No whitespaces")
    private String password;

}
