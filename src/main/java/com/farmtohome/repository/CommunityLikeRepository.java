package com.farmtohome.repository;

import com.farmtohome.model.CommunityLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityLikeRepository
        extends JpaRepository<CommunityLike, Long> {

    // Count likes for a post
    long countByPostId(Long postId);

    // Check if user already liked a post
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    // Delete all likes for a given post (for safe story deletion)
    void deleteByPostId(Long postId);
}
