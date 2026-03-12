package com.farmtohome.controller;

import com.farmtohome.model.Order;
import com.farmtohome.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Farmer: Get my orders
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<Order>> getFarmerOrders(@PathVariable Long farmerId) {
        return ResponseEntity.ok(orderService.getOrdersForFarmer(farmerId));
    }

    // Farmer: Update status
    @PostMapping("/update-status")
    public ResponseEntity<?> updateStatus(@RequestBody Map<String, Object> body) {
        try {
            Long orderId = Long.valueOf(body.get("orderId").toString());
            String status = body.get("status").toString();
            Long farmerId = Long.valueOf(body.get("farmerId").toString());

            return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status, farmerId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Customer: Place order
    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(@RequestBody com.farmtohome.dto.OrderRequest request) {
        try {
            return ResponseEntity.ok(orderService.placeOrder(request));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Customer: Cancel Order
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, @RequestBody Map<String, Long> body) {
        try {
            Long customerId = body.get("customerId");
            return ResponseEntity.ok(orderService.cancelOrder(orderId, customerId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Customer: History
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Order>> getCustomerOrders(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getCustomerOrders(customerId));
    }
}
