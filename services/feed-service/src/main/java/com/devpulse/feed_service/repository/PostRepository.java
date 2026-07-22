package com.devpulse.feed_service.repository;

import com.devpulse.feed_service.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Query
            ("""
        SELECT p FROM Post p
        WHERE p.createdAt < :cursorTime
           OR (p.createdAt = :cursorTime AND p.id < :cursorId)
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    List<Post> findFeedBefore(
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
