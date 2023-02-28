package com.kakura.icetube.repository;

import com.kakura.icetube.model.User;
import com.kakura.icetube.model.Video;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@CacheConfig(cacheNames = "video")
public interface VideoRepository extends JpaRepository<Video, Long> {

    @Cacheable(key = "#id")
    Optional<Video> findById(Long id);

    @Query("SELECT v FROM Video v JOIN FETCH v.tags")
    List<Video> findAll();

    List<Video> findAllByUserId(Long userId);

    List<Video> findByUserIn(List<User> users);

    @CacheEvict(key = "#video.id")
    Video save(Video video);

}
