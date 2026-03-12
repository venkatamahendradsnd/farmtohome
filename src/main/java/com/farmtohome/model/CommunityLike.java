package com.farmtohome.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "community_likes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"})
)
public class CommunityLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Story being liked
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    // User who liked the story (farmer or customer)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ---------- Constructors ----------
    public CommunityLike() {}

    public CommunityLike(CommunityPost post, User user) {
        this.post = post;
        this.user = user;
    }

    // ---------- Getters & Setters ----------
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CommunityPost getPost() {
        return post;
    }

    public void setPost(CommunityPost post) {
        this.post = post;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
