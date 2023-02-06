package com.kakura.icetube.mapper;

import com.kakura.icetube.dto.NewVideoDto;
import com.kakura.icetube.dto.VideoDto;
import com.kakura.icetube.model.Video;
import com.kakura.icetube.model.VideoStatus;
import org.springframework.stereotype.Component;

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
                .previewUrl("/api/v1/video/preview/" + video.getId())
                .streamUrl("/api/v1/video/stream/" + video.getId())
                .build();
    }

}
