package com.kakura.icetube.model;

import com.kakura.icetube.model.converter.AtomicIntegerConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Entity
//@Data
@Getter
@Setter
@ToString
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

    private LocalDateTime createdAt;

    private int videoResolution;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = true) //todo change to nullable = false
    private User user;

    @ManyToMany(mappedBy = "watchedVideos")
    private Set<User> usersWhoWatched = new LinkedHashSet<>();

    @ManyToMany(mappedBy = "likedVideos")
    private Set<User> usersWhoLiked = new LinkedHashSet<>();

    @Convert(converter = AtomicIntegerConverter.class)
    private AtomicInteger likes = new AtomicInteger(0);

    @Convert(converter = AtomicIntegerConverter.class)
    private AtomicInteger dislikes = new AtomicInteger(0);

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Tag> tags;

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

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Video)) {
            return false;
        }
        Video other = (Video) obj;
        return Objects.equals(id, other.id);
    }
}
