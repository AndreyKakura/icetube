package com.kakura.icetube.mapper;

import com.kakura.icetube.dto.NewVideoDto;
import com.kakura.icetube.dto.VideoDto;
import com.kakura.icetube.model.Tag;
import com.kakura.icetube.model.Video;
import com.kakura.icetube.model.VideoStatus;
import org.springframework.stereotype.Component;

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
                .previewUrl("http://localhost:8080/api/v1/video/preview/" + video.getId())
                .streamUrl("http://localhost:8080/api/v1/video/stream/" + video.getId())
                .build();
    }

}
