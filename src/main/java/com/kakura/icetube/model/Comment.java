package com.kakura.icetube.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    private Long id;

    private String text;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Integer likes;

    private Integer dislikes;

    @ManyToOne()
    @JoinColumn(name = "video_id", referencedColumnName = "id")
    private Video video;
}
