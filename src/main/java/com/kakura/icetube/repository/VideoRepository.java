package com.kakura.icetube.repository;

import com.kakura.icetube.model.Tag;
import com.kakura.icetube.model.User;
import com.kakura.icetube.model.Video;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@CacheConfig(cacheNames = "video")
public interface VideoRepository extends JpaRepository<Video, Long> {

//    @Cacheable(key = "#id")
    Optional<Video> findById(Long id);

    @Cacheable(key = "#id")
    @Query("SELECT v FROM Video v WHERE v.id = :id")
    Optional<Video> findByIdWithCache(@Param("id") Long id);

    Page<Video> findAll(Pageable pageable);

    Page<Video> findAllByUserId(Long userId, Pageable pageable);

    Page<Video> findByUserIn(List<User> users, Pageable pageable);

    Page<Video> findByUsersWhoWatchedIn(List<User> users, Pageable pageable);

    Page<Video> findByUsersWhoLikedIn(List<User> users, Pageable pageable);

    Page<Video> findByTitleContaining(String title, Pageable pageable);

    Page<Video> findByTags(Tag tag, Pageable pageable);

    @CacheEvict(key = "#video.id")
    Video save(Video video);

    @CacheEvict(key = "#id")
    void deleteById(Long id);

}
