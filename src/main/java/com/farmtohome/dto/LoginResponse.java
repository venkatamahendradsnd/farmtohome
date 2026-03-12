package com.farmtohome.dto;

public class LoginResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private Double averageRating;
    private Integer ratingCount;

    public LoginResponse() {
    }

    public LoginResponse(Long id, String name, String email, String role, Double averageRating, Integer ratingCount) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
