package com.kakura.icetube.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {

    @NotBlank(message = "Text should not be blank")
    private String text;

    private Long userId;

    private String username;

    private LocalDateTime createdAt;

}
