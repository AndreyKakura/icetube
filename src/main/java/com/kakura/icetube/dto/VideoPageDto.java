package com.kakura.icetube.dto;

import com.kakura.icetube.model.Video;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoPageDto {

    private List<VideoDto> videos;

    private Integer totalPages;

}
