package com.farmtohome.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Double averageRating = 0.0;

    @Column(nullable = false)
    private Integer ratingCount = 0;

    // Transient Badge Logic
    @Transient
    public String getBadge() {
        if (ratingCount == 0)
            return "NEW"; // New Farmer
        if (averageRating >= 4.0 && ratingCount >= 3)
            return "TRUSTED"; // Green
        if (ratingCount > 2 && averageRating > 3.0 && averageRating < 4.0)
            return "GROWING"; // Orange
        if (averageRating < 2.0 || ratingCount < 2)
            return "LOW_RATED"; // Red
        return "GROWING"; // Fallback for gaps (e.g. rating 2.5) to encourage
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
    
    // Support setting role from a String (for JSON deserialization and form data)
    @JsonSetter("role")
    public void setRoleFromString(Object roleValue) {
        if (roleValue instanceof Role) {
            this.role = (Role) roleValue;
        } else if (roleValue instanceof String) {
            String roleStr = ((String) roleValue).toUpperCase().trim();
            try {
                this.role = Role.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                // Default to CUSTOMER if invalid
                this.role = Role.CUSTOMER;
            }
        }
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }
}
