package com.farmtohome.repository;

import com.farmtohome.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByFarmerId(Long farmerId);
}
