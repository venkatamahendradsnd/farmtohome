package com.farmtohome.repository;

import com.farmtohome.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByFarmerId(Long farmerId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi JOIN oi.order o WHERE oi.product.id = :productId AND o.status IN ('PLACED', 'ACCEPTED', 'OUT_FOR_DELIVERY', 'WAITING_FOR_FARMER_APPROVAL', 'FARMER_APPROVED')")
    Integer sumPendingQuantities(@org.springframework.web.bind.annotation.RequestParam("productId") Long productId);
}
