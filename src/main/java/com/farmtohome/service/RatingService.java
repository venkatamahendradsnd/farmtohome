package com.farmtohome.service;

import com.farmtohome.model.*;
import com.farmtohome.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatingService {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private FarmerProfileRepository farmerProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    public Rating addRating(Long customerId, Long farmerId, Integer ratingValue, String review) {
        // Validation: Customer must have ordered from Farmer and order status must be
        // DELIVERED
        // For MVP, skipping strict check or just checking if any order exists.

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        User farmer = userRepository.findById(farmerId).orElseThrow(() -> new RuntimeException("Farmer not found"));

        Rating rating = new Rating();
        rating.setCustomer(customer);
        rating.setFarmer(farmer);
        rating.setRatingValue(ratingValue);
        rating.setReview(review);

        Rating savedRating = ratingRepository.save(rating);

        // Update Farmer Average Rating
        updateFarmerRating(farmerId);

        return savedRating;
    }

    private void updateFarmerRating(Long farmerId) {
        List<Rating> ratings = ratingRepository.findByFarmerId(farmerId);
        double avg = ratings.stream().mapToInt(Rating::getRatingValue).average().orElse(3.0);
        int count = ratings.size();

        FarmerProfile profile = farmerProfileRepository.findByUserId(farmerId).orElse(null);
        if (profile != null) {
            profile.setRating(avg);
            profile.setTotalReviews(count);
            farmerProfileRepository.save(profile);
        }
    }

    public List<Rating> getFarmerRatings(Long farmerId) {
        return ratingRepository.findByFarmerId(farmerId);
    }
}
