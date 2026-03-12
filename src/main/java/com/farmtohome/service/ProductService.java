package com.farmtohome.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.farmtohome.model.Product;
import com.farmtohome.model.User;
import com.farmtohome.repository.ProductRepository;
import com.farmtohome.repository.UserRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    public Product addProduct(Product product, Long farmerId) {
        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));
        product.setFarmer(farmer);
        if (product.getMinOrderQuantity() == null || product.getMinOrderQuantity() < 1) {
            product.setMinOrderQuantity(1);
        }
        if (product.getStatus() != null && !product.getStatus().trim().isEmpty()
                && java.util.Arrays.asList("IN_SEASON", "OUT_OF_STOCK", "PAUSED").contains(product.getStatus().toUpperCase().trim())) {
            product.setStatus(product.getStatus().toUpperCase().trim());
        } else {
            product.setStatus("IN_SEASON");
        }
        // Back-compat: keep imageUrl aligned with first imageUrls entry when provided
        if ((product.getImageUrl() == null || product.getImageUrl().isEmpty())
                && product.getImageUrls() != null
                && !product.getImageUrls().isEmpty()) {
            product.setImageUrl(product.getImageUrls().get(0));
        }
        return productRepository.save(product);
    }

    public Product updateProduct(Long productId, Product productInterval, Long farmerId) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!existingProduct.getFarmer().getId().equals(farmerId)) {
            throw new RuntimeException("Unauthorized");
        }

        existingProduct.setName(productInterval.getName());
        existingProduct.setCategory(productInterval.getCategory());
        existingProduct.setPrice(productInterval.getPrice());
        existingProduct.setQuantity(productInterval.getQuantity());
        existingProduct.setDescription(productInterval.getDescription());
        existingProduct.setUnit(productInterval.getUnit());
        if (productInterval.getMinOrderQuantity() != null && productInterval.getMinOrderQuantity() >= 1) {
            existingProduct.setMinOrderQuantity(productInterval.getMinOrderQuantity());
        }
        if (productInterval.getStatus() != null && !productInterval.getStatus().trim().isEmpty()) {
            String s = productInterval.getStatus().toUpperCase().trim();
            if (java.util.Arrays.asList("IN_SEASON", "OUT_OF_STOCK", "PAUSED").contains(s)) {
                existingProduct.setStatus(s);
            }
        }

        // Only update image if a new one is provided
        if (productInterval.getImageUrl() != null && !productInterval.getImageUrl().isEmpty()) {
            existingProduct.setImageUrl(productInterval.getImageUrl());
        }
        if (productInterval.getImageUrls() != null && !productInterval.getImageUrls().isEmpty()) {
            existingProduct.setImageUrls(productInterval.getImageUrls());
            existingProduct.setImageUrl(productInterval.getImageUrls().get(0));
        }

        return productRepository.save(existingProduct);
    }

    public void deleteProduct(Long productId, Long farmerId) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!existingProduct.getFarmer().getId().equals(farmerId)) {
            throw new RuntimeException("Unauthorized");
        }

        // Mark as DELETED (soft delete) so it is hidden from farmer/customer product lists.
        existingProduct.setStatus("DELETED");
        productRepository.save(existingProduct);
    }

    public List<Product> getProductsByFarmer(Long farmerId) {
        return productRepository.findByFarmerIdAndStatusIn(farmerId, java.util.Arrays.asList("IN_SEASON", "PAUSED", "OUT_OF_STOCK"));
    }

    public List<Product> getAllActiveProducts() {
        return productRepository.findByStatusIn(java.util.Arrays.asList("IN_SEASON", "PAUSED"));
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public List<Product> searchProducts(String query) {
        return productRepository.findByNameContainingIgnoreCaseAndStatusIn(query,
                java.util.Arrays.asList("IN_SEASON", "PAUSED"));
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndStatusIn(category, java.util.Arrays.asList("IN_SEASON", "PAUSED"));
    }

    public Product toggleStatus(Long productId, Long farmerId) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!existingProduct.getFarmer().getId().equals(farmerId)) {
            throw new RuntimeException("Unauthorized");
        }

        if ("IN_SEASON".equals(existingProduct.getStatus())) {
            existingProduct.setStatus("PAUSED");
        } else if ("PAUSED".equals(existingProduct.getStatus())) {
            existingProduct.setStatus("IN_SEASON");
        } else if ("OUT_OF_STOCK".equals(existingProduct.getStatus())) {
            // Allow relisting
            existingProduct.setStatus("IN_SEASON");
        }
        return productRepository.save(existingProduct);
    }

    public Product setStatus(Long productId, Long farmerId, String status) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!existingProduct.getFarmer().getId().equals(farmerId)) {
            throw new RuntimeException("Unauthorized");
        }

        String s = status == null ? "" : status.toUpperCase().trim();
        if (!java.util.Arrays.asList("IN_SEASON", "OUT_OF_STOCK", "PAUSED").contains(s)) {
            throw new RuntimeException("Invalid status. Use IN_SEASON, OUT_OF_STOCK, or PAUSED");
        }
        existingProduct.setStatus(s);
        return productRepository.save(existingProduct);
    }
}
