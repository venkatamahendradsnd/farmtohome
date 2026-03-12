package com.farmtohome.controller;

import com.farmtohome.model.Product;
import com.farmtohome.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // Public: Get all active products
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllActiveProducts();
    }

    // Public: Get product details
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product != null) {
            return ResponseEntity.ok(product);
        }
        return ResponseEntity.notFound().build();
    }

    // Farmer: Add Product
    @PostMapping("/add")
    public ResponseEntity<?> addProduct(@RequestBody Product product, @RequestParam Long farmerId) {
        try {
            return ResponseEntity.ok(productService.addProduct(product, farmerId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Farmer: Update Product
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product product,
            @RequestParam Long farmerId) {
        try {
            return ResponseEntity.ok(productService.updateProduct(id, product, farmerId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Farmer: Soft Delete Product
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, @RequestParam Long farmerId) {
        try {
            productService.deleteProduct(id, farmerId);
            return ResponseEntity.ok("Product deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Farmer: Toggle Status (Active/Paused)
    @PostMapping("/status/{id}")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id, @RequestParam Long farmerId) {
        try {
            return ResponseEntity.ok(productService.toggleStatus(id, farmerId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Farmer: Set availability status (IN_SEASON, OUT_OF_STOCK, PAUSED)
    @PostMapping("/set-status/{id}")
    public ResponseEntity<?> setStatus(@PathVariable Long id, @RequestParam Long farmerId, @RequestParam String status) {
        try {
            return ResponseEntity.ok(productService.setStatus(id, farmerId, status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Farmer: Get My Products
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<Product>> getMyProducts(@PathVariable Long farmerId) {
        return ResponseEntity.ok(productService.getProductsByFarmer(farmerId));
    }

    // Public: Search
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String query) {
        // Assuming service has this method, I need to add it to service too
        return ResponseEntity.ok(productService.searchProducts(query));
    }

    // Public: Filter by Category
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }
}
