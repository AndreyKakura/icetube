package com.kakura.icetube.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoDto {

    private Long id;

    private String title;

    private String description;

    private String contentType;

    private Set<String> tags;

    private Integer likes;

    private Integer dislikes;

    private Integer viewCount;

    private String previewUrl;

    private String streamUrl;

    private String authorName;

    private Long authorId;

    private Boolean isSubscribedToAuthor;

    private Boolean isAuthor;

    private LocalDateTime createdAt;

    private Integer videoResolution;
}
