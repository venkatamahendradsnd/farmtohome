package com.farmtohome.repository;

import com.farmtohome.model.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostRepository
        extends JpaRepository<CommunityPost, Long> {
    // No extra methods needed for now
}
