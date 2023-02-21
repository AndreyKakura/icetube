package com.kakura.icetube.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

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
    private Set<User> subscriptions;

    @ManyToMany
    private Set<User> subscribers;

    @ManyToMany
    private Set<Video> likedVideos = new LinkedHashSet<>();

    @ManyToMany
    private Set<Video> dislikedVideos = new LinkedHashSet<>();

    @ManyToMany
    private Set<Video> watchedVideos = new LinkedHashSet<>();

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
}
