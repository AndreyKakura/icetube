package com.kakura.icetube.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    @Id
    private Long id;

    private String title;

    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Integer likes;

    private Integer dislikes;

    @ManyToMany
    private Set<Tag> tags;

    private String videoUrl;

    @Enumerated(EnumType.STRING)
    private VideoStatus videoStatus;

    private Integer viewCount;

    @OneToMany(/*cascade = {CascadeType.REMOVE, CascadeType.MERGE},*/ mappedBy = "video", fetch = FetchType.LAZY)
    private List<Comment> comments;

}
