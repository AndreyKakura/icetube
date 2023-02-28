package com.kakura.icetube.model;

import com.kakura.icetube.model.converter.AtomicIntegerConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Collection<Role> roles = new ArrayList<>();

    @Column(unique = true, length = 30, nullable = false)
    private String username;

    @Column(length = 30, nullable = false)
    private String name;

    @Column(length = 30, nullable = false)
    private String surname;

    @Column(nullable = false)
    private String password;

    @ManyToMany
    private Set<Video> likedVideos = new LinkedHashSet<>();

    @ManyToMany
    private Set<Video> dislikedVideos = new LinkedHashSet<>();

    @ManyToMany
    private Set<Video> watchedVideos = new LinkedHashSet<>();

    @Convert(converter = AtomicIntegerConverter.class)
    private AtomicInteger subscribersCount = new AtomicInteger(0);

    public void addToLikedVideos(Video video) {
        likedVideos.add(video);
    }

    public void removeFromLikedVideos(Video video) {
        likedVideos.remove(video);
    }

    public void removeFromDislikedVideos(Video video) {
        dislikedVideos.remove(video);
    }

    public void addToDislikedVideos(Video video) {
        dislikedVideos.add(video);
    }

    public void addToWatchedVideos(Video videoFromDb) {
        watchedVideos.add(videoFromDb);
    }

    public void incrementSubscribersCount() {
        subscribersCount.incrementAndGet();
    }

    public void decrementSubscribersCount() {
        subscribersCount.decrementAndGet();
    }

}
