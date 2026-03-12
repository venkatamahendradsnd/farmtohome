package com.farmtohome.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.farmtohome.model.Cart;
import com.farmtohome.model.CartItem;
import com.farmtohome.model.Product;
import com.farmtohome.model.User;
import com.farmtohome.repository.CartItemRepository;
import com.farmtohome.repository.CartRepository;
import com.farmtohome.repository.ProductRepository;
import com.farmtohome.repository.UserRepository;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    public Cart getCart(Long customerId) {
        return cartRepository.findByCustomerId(customerId).orElseGet(() -> {
            User customer = userRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            Cart newCart = new Cart();
            newCart.setCustomer(customer);
            return cartRepository.save(newCart);
        });
    }

    public void updateCartItemQuantity(Long cartItemId, Integer quantity) {
        if (quantity <= 0) {
            cartItemRepository.deleteById(cartItemId);
        } else {
            CartItem item = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> new RuntimeException("Item not found"));
            Product p = item.getProduct();
            if (p == null) {
                throw new RuntimeException("Product not found");
            }
            int available = (p != null && p.getQuantity() != null) ? p.getQuantity() : 0;
            if (available <= 0 || "OUT_OF_STOCK".equals(p.getStatus()) || "PAUSED".equals(p.getStatus()) || "DELETED".equals(p.getStatus())) {
                throw new RuntimeException("Product is no longer available: " + p.getName());
            }
            int moq = (p != null && p.getMinOrderQuantity() != null && p.getMinOrderQuantity() >= 1)
                    ? p.getMinOrderQuantity()
                    : 1;
            int boundedQty = Math.min(quantity, available);
            if (boundedQty < moq) {
                boundedQty = Math.min(moq, available);
            }
            item.setQuantity(boundedQty);
            cartItemRepository.save(item);
        }
    }

    public void addToCart(Long customerId, Long productId, Integer quantity) {
        Cart cart = getCart(customerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Prevent adding deleted/out-of-stock or paused products
        if ("DELETED".equals(product.getStatus())) {
            throw new RuntimeException("Product is no longer available: " + product.getName());
        }
        if ("OUT_OF_STOCK".equals(product.getStatus()) || product.getQuantity() == null || product.getQuantity() <= 0) {
            throw new RuntimeException("Product is out of stock: " + product.getName());
        }
        if ("PAUSED".equals(product.getStatus())) {
            throw new RuntimeException("Product is temporarily unavailable: " + product.getName());
        }

        int moq = (product.getMinOrderQuantity() != null && product.getMinOrderQuantity() >= 1)
                ? product.getMinOrderQuantity()
                : 1;
        int addQty = quantity == null ? moq : Math.max(quantity, moq);

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int available = product.getQuantity() == null ? 0 : product.getQuantity();
            int nextQty = Math.min(item.getQuantity() + addQty, available);
            if (nextQty <= 0) {
                throw new RuntimeException("Product is out of stock: " + product.getName());
            }
            item.setQuantity(nextQty);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            int available = product.getQuantity() == null ? 0 : product.getQuantity();
            if (available <= 0) {
                throw new RuntimeException("Product is out of stock: " + product.getName());
            }
            newItem.setQuantity(Math.min(addQty, available));
            cartItemRepository.save(newItem);
        }
    }

    public void removeFromCart(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    public java.util.List<CartItem> getCartItems(Long customerId) {
        Cart cart = getCart(customerId);
        return cartItemRepository.findByCartId(cart.getId());
    }
}
