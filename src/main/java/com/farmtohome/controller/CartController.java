package com.farmtohome.controller;

import com.farmtohome.model.CartItem;
import com.farmtohome.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/{customerId}")
    public ResponseEntity<List<CartItem>> getCart(@PathVariable Long customerId) {
        return ResponseEntity.ok(cartService.getCartItems(customerId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> payload) {
        Long customerId = ((Number) payload.get("customerId")).longValue();
        Long productId = ((Number) payload.get("productId")).longValue();
        Integer quantity = ((Number) payload.get("quantity")).intValue();
        cartService.addToCart(customerId, productId, quantity);
        return ResponseEntity.ok("Added");
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateQuantity(@RequestBody Map<String, Object> payload) {
        Long cartItemId = ((Number) payload.get("cartItemId")).longValue();
        Integer quantity = ((Number) payload.get("quantity")).intValue();
        cartService.updateCartItemQuantity(cartItemId, quantity);
        return ResponseEntity.ok("Updated");
    }

    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long cartItemId) {
        cartService.removeFromCart(cartItemId);
        return ResponseEntity.ok("Removed from cart");
    }
}
