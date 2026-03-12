package com.farmtohome.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.farmtohome.model.ChatThread;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Long> {
    Optional<ChatThread> findByFarmer_IdAndCustomer_Id(Long farmerId, Long customerId);

    List<ChatThread> findByFarmer_IdOrderByUpdatedAtDesc(Long farmerId);

    List<ChatThread> findByCustomer_IdOrderByUpdatedAtDesc(Long customerId);

    // Convenience: all threads where this user is either farmer or customer
    List<ChatThread> findByFarmer_IdOrCustomer_IdOrderByUpdatedAtDesc(Long farmerId, Long customerId);
}
