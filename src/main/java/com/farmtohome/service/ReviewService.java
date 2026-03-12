package com.farmtohome.service;

import com.farmtohome.dto.ReviewDTO;
import com.farmtohome.model.Order;
import com.farmtohome.model.OrderStatus; // Ensure enum visibility
import com.farmtohome.model.Review;
import com.farmtohome.model.User;
import com.farmtohome.repository.OrderRepository;
import com.farmtohome.repository.ReviewRepository;
import com.farmtohome.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private com.farmtohome.repository.ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void addReview(ReviewDTO dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getStatus().toString().equals("DELIVERED")) {
            throw new RuntimeException("Only delivered orders can be rated");
        }

        if (dto.getProductId() != null) {
            if (reviewRepository.existsByOrderIdAndProductId(order.getId(), dto.getProductId())) {
                throw new RuntimeException("You have already reviewed this product in this order");
            }
        } else {
             // Fallback for old behavior? Or strictly require product? 
             // Requirement says reviews are for products.
             throw new RuntimeException("Product ID is required for review");
        }

        User customer = userRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        com.farmtohome.model.Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Verify product was in order
        boolean productInOrder = order.getItems().stream()
            .anyMatch(item -> item.getProduct().getId().equals(dto.getProductId()));
        
        if(!productInOrder) {
             throw new RuntimeException("This product was not part of your order");
        }

        User farmer = product.getFarmer();

        Review review = new Review();
        review.setCustomer(customer);
        review.setFarmer(farmer);
        review.setOrder(order);
        review.setProduct(product);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        
        reviewRepository.save(review);

        // Update Farmer Stats
        updateFarmerRating(farmer, dto.getRating());
    }

    private void updateFarmerRating(User farmer, int newRating) {
        int currentCount = farmer.getRatingCount();
        double currentAvg = farmer.getAverageRating();

        double totalScore = (currentAvg * currentCount) + newRating;
        int newCount = currentCount + 1;
        double newAvg = totalScore / newCount;

        farmer.setRatingCount(newCount);
        farmer.setAverageRating(Math.round(newAvg * 10.0) / 10.0); // Round to 1 decimal

        userRepository.save(farmer);
    }

    public List<Review> getFarmerReviews(Long farmerId) {
        return reviewRepository.findByFarmerId(farmerId);
    }

    public List<Review> getProductReviews(Long productId) {
        return reviewRepository.findByProductId(productId);
    }
}
