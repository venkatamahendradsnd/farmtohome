package com.farmtohome.controller;

import com.farmtohome.dto.ReviewDTO;
import com.farmtohome.model.Review;
import com.farmtohome.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/add")
    public ResponseEntity<?> addReview(@RequestBody ReviewDTO dto) {
        try {
            reviewService.addReview(dto);
            return ResponseEntity.ok("Review added successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/farmer/{id}")
    public List<Review> getFarmerReviews(@PathVariable Long id) {
        return reviewService.getFarmerReviews(id);
    }

    @GetMapping("/product/{id}")
    public List<Review> getProductReviews(@PathVariable Long id) {
        return reviewService.getProductReviews(id);
    }
}
