package com.farmtohome.controller;

import com.farmtohome.model.Rating;
import com.farmtohome.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    @PostMapping("/add")
    public ResponseEntity<?> addRating(@RequestBody Map<String, Object> body) {
        try {
            Long customerId = Long.valueOf(body.get("customerId").toString());
            Long farmerId = Long.valueOf(body.get("farmerId").toString());
            Integer ratingValue = Integer.valueOf(body.get("rating").toString());
            String review = body.get("review").toString();

            return ResponseEntity.ok(ratingService.addRating(customerId, farmerId, ratingValue, review));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<?> getRatings(@PathVariable Long farmerId) {
        return ResponseEntity.ok(ratingService.getFarmerRatings(farmerId));
    }
}
