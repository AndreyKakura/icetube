package com.kakura.icetube.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String videoFileName;

    private String previewFileName;

    private String videoContentType;

    private String previewContentType;

    private Long fileSize;

    private Long videoLength;

    private String title;

    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = true) //todo change to nullable = false
    private User user;

    private Integer likes;

    private Integer dislikes;

    @ManyToMany
    private Set<Tag> tags;


    @Enumerated(EnumType.STRING)
    private VideoStatus videoStatus;

    private Integer viewCount;

    @OneToMany(/*cascade = {CascadeType.REMOVE, CascadeType.MERGE},*/ mappedBy = "video", fetch = FetchType.LAZY)
    private List<Comment> comments;

}
