package com.farmtohome.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.farmtohome.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByFarmerId(Long farmerId);

    List<Product> findByFarmerIdAndStatusIn(Long farmerId, List<String> statuses);

    List<Product> findByStatus(String status);

    List<Product> findByCategoryAndStatus(String category, String status);

    List<Product> findByNameContainingIgnoreCaseAndStatus(String name, String status);

    List<Product> findByCategoryAndStatusIn(String category, List<String> statuses);

    List<Product> findByNameContainingIgnoreCaseAndStatusIn(String name, List<String> statuses);

    List<Product> findByStatusIn(List<String> statuses);

    void deleteByFarmerId(Long farmerId);
}
