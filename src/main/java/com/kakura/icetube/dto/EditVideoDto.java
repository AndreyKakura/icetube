package com.kakura.icetube.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditVideoDto {

    private Long id;

    private String title;

    private String description;

    private String previewUrl;

    private String streamUrl;

    private Set<String> tags;

}
