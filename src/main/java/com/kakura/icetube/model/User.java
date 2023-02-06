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
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long username;

    private String name;

    private String surname;

    @ManyToMany
    private Set<User> subscriptions;

    @ManyToMany
    private Set<User> subscribers;

    @ManyToMany
    private List<Video> likedVideos;

    @ManyToMany
    private List<Video> dislikedVideos;
}
