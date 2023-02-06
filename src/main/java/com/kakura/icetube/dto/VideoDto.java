package com.kakura.icetube.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoDto {

    private Long id;

    private String title;

    private String description;

    private String contentType;

    private String previewUrl;

    private String streamUrl;

}
