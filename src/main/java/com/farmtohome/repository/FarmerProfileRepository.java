package com.farmtohome.repository;

import com.farmtohome.model.FarmerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FarmerProfileRepository extends JpaRepository<FarmerProfile, Long> {
    Optional<FarmerProfile> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
