package com.kakura.icetube.mapper;

import com.kakura.icetube.dto.NewVideoDto;
import com.kakura.icetube.dto.VideoDto;
import com.kakura.icetube.model.Tag;
import com.kakura.icetube.model.Video;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class VideoMapper {
    public Video toModel(NewVideoDto newVideoDto) {
        return Video.builder()
                .videoFileName(newVideoDto.getVideoFile().getOriginalFilename())
                .previewFileName(newVideoDto.getPreviewFile().getOriginalFilename())
                .videoContentType(newVideoDto.getVideoFile().getContentType())
                .previewContentType(newVideoDto.getPreviewFile().getContentType().replace("image/", "."))
                .fileSize(newVideoDto.getVideoFile().getSize())
                .title(newVideoDto.getTitle())
                .description(newVideoDto.getDescription())
                .createdAt(LocalDateTime.now())
                .likes(new AtomicInteger(0))
                .dislikes(new AtomicInteger(0))
                .viewCount(new AtomicInteger(0))
                .build();
    }

    public VideoDto toDto(Video video) {
        return VideoDto.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .contentType(video.getVideoContentType())
                .tags(video.getTags().stream().map(Tag::getTagText).collect(Collectors.toSet()))
                .likes(video.getLikes().get())
                .dislikes(video.getDislikes().get())
                .viewCount(video.getViewCount().get())
                .previewUrl("/api/video/preview/" + video.getId())
                .streamUrl("/api/video/stream/" + video.getId())
                .authorName(video.getUser().getUsername())
                .authorId(video.getUser().getId())
                .createdAt(video.getCreatedAt())
                .build();
    }

}
