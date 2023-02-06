package com.kakura.icetube.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewVideoDto {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String videoStatus;

    private Set<String> tags;

    @NotNull
    private MultipartFile videoFile;

    @NotNull
    private MultipartFile previewFile;
}
