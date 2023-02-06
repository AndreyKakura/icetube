package com.kakura.icetube.repository;

import com.kakura.icetube.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByTagText(String tagText);
}
