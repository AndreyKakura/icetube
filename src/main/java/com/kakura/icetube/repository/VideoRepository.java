package com.kakura.icetube.repository;

import com.kakura.icetube.model.Video;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@CacheConfig(cacheNames = "video")
public interface VideoRepository extends JpaRepository<Video, Long> {

    @Cacheable(key = "#id")
    Optional<Video> findById(Long id);

    @CacheEvict(key = "#result.id")
    Video save(Video video);

}
