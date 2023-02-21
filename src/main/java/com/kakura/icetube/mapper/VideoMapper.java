package com.kakura.icetube.mapper;

import com.kakura.icetube.dto.NewVideoDto;
import com.kakura.icetube.dto.VideoDto;
import com.kakura.icetube.model.Tag;
import com.kakura.icetube.model.Video;
import com.kakura.icetube.model.VideoStatus;
import org.springframework.stereotype.Component;

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
                .likes(new AtomicInteger(0))
                .dislikes(new AtomicInteger(0))
                .viewCount(new AtomicInteger(0))
                .videoStatus(VideoStatus.valueOf(newVideoDto.getVideoStatus()))
                .build();
    }

    public VideoDto toDto(Video video) {
        return VideoDto.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .contentType(video.getVideoContentType())
                .tags(video.getTags().stream().map(Tag::getTagText).collect(Collectors.toSet()))
                .videoStatus(video.getVideoStatus().name())
                .likes(video.getLikes().get())
                .dislikes(video.getDislikes().get())
                .viewCount(video.getViewCount().get())
                .previewUrl("/api/video/preview/" + video.getId())
                .streamUrl("/api/video/stream/" + video.getId())
                .build();
    }

}
