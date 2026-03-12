package com.farmtohome.controller;

import com.farmtohome.model.FarmerProfile;
import com.farmtohome.model.User;
import com.farmtohome.repository.FarmerProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/farmer")
public class FarmerController {

    @Autowired
    private FarmerProfileRepository farmerProfileRepository;

    @Autowired
    private com.farmtohome.repository.UserRepository userRepository;

    @Autowired
    private com.farmtohome.service.OrderService orderService;

    @Autowired
    private com.farmtohome.service.ProductService productService;

    @Autowired
    private com.farmtohome.repository.ProductRepository productRepository;

    @Autowired
    private com.farmtohome.repository.OrderItemRepository orderItemRepository;

    @Autowired
    private com.farmtohome.repository.ChatThreadRepository chatThreadRepository;

    @Autowired
    private com.farmtohome.repository.ChatMessageRepository chatMessageRepository;

    @Autowired
    private com.farmtohome.repository.CartRepository cartRepository;

    @Autowired
    private com.farmtohome.repository.CartItemRepository cartItemRepository;

    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable Long userId) {
        FarmerProfile profile = farmerProfileRepository.findByUserId(userId).orElse(null);
        if (profile != null) {
            return ResponseEntity.ok(profile);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/analytics/{userId}")
    public ResponseEntity<?> getAnalytics(@PathVariable Long userId) {
        // Calculate Analytics
        java.util.List<com.farmtohome.model.Order> orders = orderService.getOrdersForFarmer(userId);

        java.math.BigDecimal totalSales = java.math.BigDecimal.ZERO;
        int activeOrders = 0;

        for (com.farmtohome.model.Order order : orders) {
            if ("DELIVERED".equals(order.getStatus())) {
                totalSales = totalSales.add(order.getTotalAmount());
            } else if (java.util.Arrays.asList(
                    "PLACED",
                    "ACCEPTED",
                    "OUT_FOR_DELIVERY",
                    "WAITING_FOR_FARMER_APPROVAL",
                    "FARMER_APPROVED"
            ).contains(order.getStatus())) {
                activeOrders++;
            }
        }

        FarmerProfile profile = farmerProfileRepository.findByUserId(userId).orElse(null);
        com.farmtohome.model.User user = profile != null ? profile.getUser() : null;
        double rating = user != null ? user.getAverageRating() : 0.0;
        int reviews = user != null ? user.getRatingCount() : 0;

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("totalSales", totalSales);
        response.put("activeOrders", activeOrders);
        response.put("rating", rating);
        response.put("totalReviews", reviews);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody FarmerProfile profile) {
        return ResponseEntity.ok(farmerProfileRepository.save(profile));
    }

    // Farmer: update my profile fields (creates profile if missing)
    @PostMapping("/profile/{userId}")
    public ResponseEntity<?> upsertProfile(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        FarmerProfile profile = farmerProfileRepository.findByUserId(userId).orElseGet(() -> {
            FarmerProfile fp = new FarmerProfile();
            fp.setUser(user);
            return fp;
        });

        if (body.containsKey("farmName")) profile.setFarmName(body.get("farmName") == null ? null : body.get("farmName").toString());
        if (body.containsKey("state")) profile.setState(body.get("state") == null ? null : body.get("state").toString());
        if (body.containsKey("district")) profile.setDistrict(body.get("district") == null ? null : body.get("district").toString());
        if (body.containsKey("pickupAddress")) profile.setPickupAddress(body.get("pickupAddress") == null ? null : body.get("pickupAddress").toString());
        if (body.containsKey("photoUrl")) profile.setPhotoUrl(body.get("photoUrl") == null ? null : body.get("photoUrl").toString());
        if (body.containsKey("story")) profile.setStory(body.get("story") == null ? null : body.get("story").toString());

        return ResponseEntity.ok(farmerProfileRepository.save(profile));
    }

    // Customer: pickup addresses for farmers in current cart
    @GetMapping("/pickup-addresses/{customerId}")
    public ResponseEntity<?> getPickupAddresses(@PathVariable Long customerId) {
        com.farmtohome.model.Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        List<com.farmtohome.model.CartItem> items = cartItemRepository.findByCartId(cart.getId());
        Map<Long, String> byFarmer = new java.util.LinkedHashMap<>();
        for (com.farmtohome.model.CartItem ci : items) {
            if (ci.getProduct() == null || ci.getProduct().getFarmer() == null) continue;
            Long fid = ci.getProduct().getFarmer().getId();
            if (byFarmer.containsKey(fid)) continue;
            FarmerProfile fp = farmerProfileRepository.findByUserId(fid).orElse(null);
            String addr = fp != null ? fp.getPickupAddress() : null;
            byFarmer.put(fid, addr);
        }
        List<Map<String, Object>> response = byFarmer.entrySet().stream().map(e -> {
            User farmer = userRepository.findById(e.getKey()).orElse(null);
            FarmerProfile fp = farmerProfileRepository.findByUserId(e.getKey()).orElse(null);
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("farmerId", e.getKey());
            row.put("farmerName", farmer != null ? farmer.getName() : "Farmer");
            row.put("farmName", fp != null ? fp.getFarmName() : null);
            row.put("pickupAddress", e.getValue());
            return row;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // Farmer: delete account (allowed only when no order history exists)
    @DeleteMapping("/account/{userId}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deleteFarmerAccount(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != com.farmtohome.model.Role.FARMER) {
            return ResponseEntity.badRequest().body("Only farmer account can be deleted here");
        }
        if (!orderItemRepository.findByFarmerId(userId).isEmpty()) {
            return ResponseEntity.badRequest().body("Cannot delete account: order history exists for this farmer");
        }

        // Remove chat data first to satisfy FK constraints
        java.util.List<com.farmtohome.model.ChatThread> threads = chatThreadRepository
                .findByFarmer_IdOrCustomer_IdOrderByUpdatedAtDesc(userId, userId);
        for (com.farmtohome.model.ChatThread t : threads) {
            chatMessageRepository.deleteByThreadId(t.getId());
            chatThreadRepository.deleteById(t.getId());
        }

        // Remove products and profile, then user
        productRepository.deleteByFarmerId(userId);
        farmerProfileRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
        return ResponseEntity.ok("Account deleted successfully");
    }
}
