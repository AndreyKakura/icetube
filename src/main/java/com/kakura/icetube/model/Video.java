package com.kakura.icetube.model;

import com.kakura.icetube.model.converter.AtomicIntegerConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Convert(converter = AtomicIntegerConverter.class)
    private AtomicInteger likes = new AtomicInteger(0);

    @Convert(converter = AtomicIntegerConverter.class)
    private AtomicInteger dislikes = new AtomicInteger(0);

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Tag> tags;


    @Enumerated(EnumType.STRING)
    private VideoStatus videoStatus;

    @Convert(converter = AtomicIntegerConverter.class)
    private AtomicInteger viewCount = new AtomicInteger(0);

    @OneToMany(/*cascade = {CascadeType.REMOVE, CascadeType.MERGE},*/ mappedBy = "video", fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    public void incrementLikes() {
        likes.incrementAndGet();
    }

    public void decrementLikes() {
        likes.decrementAndGet();
    }

    public void incrementDislikes() {
        dislikes.incrementAndGet();
    }

    public void decrementDislikes() {
        dislikes.decrementAndGet();
    }

    public void incrementViewCount() {
        viewCount.incrementAndGet();
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }
}
