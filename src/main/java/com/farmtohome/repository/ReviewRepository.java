package com.farmtohome.repository;

import com.farmtohome.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByFarmerId(Long farmerId);

    List<Review> findByProductId(Long productId);

    boolean existsByOrderIdAndProductId(Long orderId, Long productId);
}
