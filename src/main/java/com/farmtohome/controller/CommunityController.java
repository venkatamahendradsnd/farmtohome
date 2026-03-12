package com.farmtohome.controller;

import com.farmtohome.model.CommunityLike;
import com.farmtohome.model.CommunityPost;
import com.farmtohome.model.User;
import com.farmtohome.repository.CommunityLikeRepository;
import com.farmtohome.repository.CommunityPostRepository;
import com.farmtohome.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private CommunityLikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    // --------------------------------------------------
    // 1️⃣ Get all community stories (for customers & farmers)
    // --------------------------------------------------
    @GetMapping
    public List<CommunityPost> getAllPosts() {
        List<CommunityPost> posts = postRepository.findAll();

        // Set like count for each post
        posts.forEach(post ->
                post.setLikeCount(
                        likeRepository.countByPostId(post.getId())
                )
        );

        return posts;
    }

    // --------------------------------------------------
    // 2️⃣ Farmer creates a new story
    // --------------------------------------------------
    @PostMapping("/post")
    public CommunityPost createPost(@RequestBody CommunityPost post) {
        // createdAt is auto-set in entity
        return postRepository.save(post);
    }

    // --------------------------------------------------
    // 3️⃣ Like a story (customer or farmer)
    // --------------------------------------------------
    @PostMapping("/like/{postId}")
    public void likePost(
            @PathVariable Long postId,
            @RequestParam Long userId
    ) {
        // Prevent duplicate likes
        if (likeRepository.existsByPostIdAndUserId(postId, userId)) {
            return;
        }

        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CommunityLike like = new CommunityLike(post, user);
        likeRepository.save(like);
    }

    // --------------------------------------------------
    // 4️⃣ Edit a story (ONLY by the farmer who posted it)
    // --------------------------------------------------
    @PutMapping("/post/{postId}")
    public CommunityPost updatePost(
            @PathVariable Long postId,
            @RequestBody CommunityPost updatedPost
    ) {
        CommunityPost existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Update allowed fields
        existingPost.setTitle(updatedPost.getTitle());
        existingPost.setDescription(updatedPost.getDescription());
        existingPost.setImageUrl(updatedPost.getImageUrl());

        return postRepository.save(existingPost);
    }

    // --------------------------------------------------
    // 5️⃣ Delete a story (ONLY by the farmer who posted it)
    // --------------------------------------------------
    @Transactional
    @DeleteMapping("/post/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId, @RequestParam Long userId) {
        CommunityPost post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }

        if (post.getFarmer() == null || !post.getFarmer().getId().equals(userId)) {
            return ResponseEntity.status(403).body("You can delete only your own stories");
        }

        // Remove likes first to avoid FK constraint errors
        likeRepository.deleteByPostId(postId);
        postRepository.deleteById(postId);
        return ResponseEntity.ok("Story deleted successfully");
    }


}
