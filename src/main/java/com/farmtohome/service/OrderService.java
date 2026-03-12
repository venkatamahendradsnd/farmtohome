package com.farmtohome.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.farmtohome.model.Address;
import com.farmtohome.model.Cart;
import com.farmtohome.model.CartItem;
import com.farmtohome.model.Order;
import com.farmtohome.model.OrderItem;
import com.farmtohome.model.OrderStatus;
import com.farmtohome.model.Product;
import com.farmtohome.model.User;
import com.farmtohome.repository.AddressRepository;
import com.farmtohome.repository.CartItemRepository;
import com.farmtohome.repository.CartRepository;
import com.farmtohome.repository.FarmerProfileRepository;
import com.farmtohome.repository.OrderItemRepository;
import com.farmtohome.repository.OrderRepository;
import com.farmtohome.repository.ProductRepository;
import com.farmtohome.repository.UserRepository;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FarmerProfileRepository farmerProfileRepository;

    private void restoreStockForOrder(Order order) {
        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) return;
        for (OrderItem item : items) {
            Product p = item.getProduct();
            int current = p.getQuantity() == null ? 0 : p.getQuantity();
            p.setQuantity(current + item.getQuantity());
            if ("OUT_OF_STOCK".equals(p.getStatus()) && p.getQuantity() > 0) {
                p.setStatus("IN_SEASON");
            }
            productRepository.save(p);
        }
        order.setStockDeducted(false);
    }

    // Farmer: Get orders involving this farmer
    // Since we decided to split orders by farmer on checkout, we can just find
    // Orders where one of the items belongs to the farmer?
    // Actually, if we split orders by farmer, we can add 'farmer_id' to the Order
    // table to make looking up easier?
    // Or we can rely on `OrderItem` having farmer_id and join.
    // However, if we split orders, each Order will only have items from ONE farmer.
    // So distinct Order IDs from OrderItems where farmer_id = X.

    public List<Order> getOrdersForFarmer(Long farmerId) {
        List<OrderItem> items = orderItemRepository.findByFarmerId(farmerId);
        return items.stream().map(OrderItem::getOrder).distinct().collect(Collectors.toList());
    }

    public Order updateOrderStatus(Long orderId, String status, Long farmerId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify this order belongs to the farmer (check first item)
        OrderItem firstItem = orderItemRepository.findByFarmerId(farmerId).stream()
                .filter(item -> item.getOrder().getId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Access Denied: This order does not belong to you"));

        String next = status == null ? null : status.toUpperCase().trim();
        if (next == null || next.isEmpty()) {
            throw new RuntimeException("Invalid status");
        }
        // Only allow the simplified set
        try {
            OrderStatus.valueOf(next);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unsupported status: " + next);
        }

        // Simple transition validation (farmer-driven)
        String current = order.getStatus() == null ? OrderStatus.PLACED.name() : order.getStatus();
        // Back-compat for existing data
        if ("WAITING_FOR_FARMER_APPROVAL".equals(current)) current = OrderStatus.PLACED.name();
        if ("FARMER_APPROVED".equals(current)) current = OrderStatus.ACCEPTED.name();
        if ("REJECTED".equals(current)) current = OrderStatus.CANCELLED.name();
        if (OrderStatus.DELIVERED.name().equals(current) || OrderStatus.CANCELLED.name().equals(current)) {
            throw new RuntimeException("Order cannot be updated from status: " + current);
        }
        if (OrderStatus.ACCEPTED.name().equals(next) && !OrderStatus.PLACED.name().equals(current)) {
            throw new RuntimeException("Only PLACED orders can be ACCEPTED");
        }
        if (OrderStatus.OUT_FOR_DELIVERY.name().equals(next) && !OrderStatus.ACCEPTED.name().equals(current)) {
            throw new RuntimeException("Only ACCEPTED orders can be marked OUT_FOR_DELIVERY");
        }
        if (OrderStatus.DELIVERED.name().equals(next) && !OrderStatus.OUT_FOR_DELIVERY.name().equals(current)) {
            throw new RuntimeException("Only OUT_FOR_DELIVERY orders can be marked DELIVERED");
        }

        if (OrderStatus.DELIVERED.name().equals(next) && !OrderStatus.DELIVERED.name().equals(current)
                && !Boolean.TRUE.equals(order.getStockDeducted())) {
            // Back-compat: old orders created before stock reservation.
            List<OrderItem> items = orderItemRepository.findByFarmerId(farmerId).stream()
                    .filter(item -> item.getOrder().getId().equals(orderId))
                    .collect(Collectors.toList());
            for (OrderItem item : items) {
                Product p = item.getProduct();
                int currentQty = p.getQuantity() == null ? 0 : p.getQuantity();
                int nextQty = Math.max(0, currentQty - item.getQuantity());
                p.setQuantity(nextQty);
                if (nextQty == 0) {
                    p.setStatus("OUT_OF_STOCK");
                }
                productRepository.save(p);
            }
            order.setStockDeducted(true);
        }

        if (OrderStatus.CANCELLED.name().equals(next) && !OrderStatus.CANCELLED.name().equals(current)
                && Boolean.TRUE.equals(order.getStockDeducted())) {
            restoreStockForOrder(order);
        }

        order.setStatus(next);
        return orderRepository.save(order);
    }

    public List<Order> getCustomerOrders(Long customerId) {
        return orderRepository.findByCustomerIdWithItems(customerId);
    }

    @Transactional
    public List<Order> placeOrder(com.farmtohome.dto.OrderRequest request) {
        Long customerId = request.getCustomerId();
        Long addressId = request.getAddressId();
        String paymentMethod = request.getPaymentMethod() == null ? "COD" : request.getPaymentMethod().toUpperCase().trim();

        // Validate
        if (customerId == null)
            throw new RuntimeException("Invalid Customer");

        String deliveryType = request.getDeliveryType() == null ? "DELIVERY" : request.getDeliveryType().toUpperCase().trim();
        if (!"DELIVERY".equals(deliveryType) && !"PICKUP".equals(deliveryType)) {
            throw new RuntimeException("Invalid deliveryType. Use DELIVERY or PICKUP.");
        }
        if (!java.util.Arrays.asList("UPI", "CARD", "COD").contains(paymentMethod)) {
            throw new RuntimeException("Invalid paymentMethod. Use UPI, CARD, or COD.");
        }

        Cart cart = cartRepository.findByCustomerId(customerId).orElseThrow(() -> new RuntimeException("Cart empty"));
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Address address = null;
        String deliveryAddressText = null;
        if ("DELIVERY".equals(deliveryType)) {
            if (addressId == null) throw new RuntimeException("Address is required for DELIVERY");
            address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("Address not found"));
            deliveryAddressText = String.format("%s, %s, %s - %s",
                    address.getArea(), address.getDistrict(), address.getState(), address.getPincode());
        } else {
            if (request.getDeliveryAddress() == null || request.getDeliveryAddress().trim().isEmpty()) {
                throw new RuntimeException("Pickup address is required for PICKUP");
            }
            deliveryAddressText = request.getDeliveryAddress().trim();
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Group items by Farmer
        Map<User, List<CartItem>> itemsByFarmer = cartItems.stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getFarmer()));

        List<Order> createdOrders = new ArrayList<>();

        for (Map.Entry<User, List<CartItem>> entry : itemsByFarmer.entrySet()) {
            User farmer = entry.getKey();
            List<CartItem> farmerItems = entry.getValue();

            Order order = new Order();
            order.setCustomer(customer);
            order.setAddress(address); // nullable for PICKUP
            order.setDeliveryType(deliveryType);
            String preferredSlot = request.getPreferredSlot();
            order.setPreferredSlot(preferredSlot == null || preferredSlot.trim().isEmpty() ? null : preferredSlot.trim());
            String addressText = deliveryAddressText;
            if ("PICKUP".equals(deliveryType)) {
                com.farmtohome.model.FarmerProfile fp = farmerProfileRepository.findByUserId(farmer.getId()).orElse(null);
                if (fp != null && fp.getPickupAddress() != null && !fp.getPickupAddress().trim().isEmpty()) {
                    addressText = fp.getPickupAddress().trim();
                }
            }
            order.setDeliveryAddress(addressText);
            order.setPaymentMethod(paymentMethod);
            order.setStockDeducted(false);
            if(request.getContactNumber() != null) {
                order.setContactNumber(request.getContactNumber());
            }

            // Simplified required status set
            order.setStatus(OrderStatus.PLACED.name());
            order.setCreatedAt(LocalDateTime.now());

            // TODO: Store Payment Details (For now, we just proceed as if paid)

            BigDecimal infoTotal = BigDecimal.ZERO;

            Order savedOrder = orderRepository.save(order);

            for (CartItem ci : farmerItems) {
                // Check product status - reject if DELETED/OUT_OF_STOCK/PAUSED
                Product product = ci.getProduct();
                if ("DELETED".equals(product.getStatus())) {
                    throw new RuntimeException("Product no longer available: " + product.getName() + ". Please remove it from your cart and try again.");
                }
                if ("OUT_OF_STOCK".equals(product.getStatus())) {
                    throw new RuntimeException("Product out of stock: " + product.getName() + ". Please remove it from your cart and try again.");
                }
                if ("PAUSED".equals(product.getStatus())) {
                    throw new RuntimeException("Product no longer available: " + product.getName() + ". Please remove it from your cart and try again.");
                }
                if (product.getQuantity() == null || product.getQuantity() <= 0) {
                    throw new RuntimeException("Product out of stock: " + product.getName() + ". Please remove it from your cart and try again.");
                }

                int moq = (product.getMinOrderQuantity() != null && product.getMinOrderQuantity() >= 1)
                        ? product.getMinOrderQuantity()
                        : 1;
                if (ci.getQuantity() < moq) {
                    throw new RuntimeException("Minimum order quantity for " + product.getName() + " is " + moq);
                }

                OrderItem oi = new OrderItem();
                oi.setOrder(savedOrder);
                oi.setProduct(product);
                oi.setFarmer(farmer);
                oi.setQuantity(ci.getQuantity());
                oi.setPrice(product.getPrice());

                // Reserve stock immediately at order placement.
                int currentQty = product.getQuantity() == null ? 0 : product.getQuantity();
                if (currentQty < ci.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }
                int nextQty = currentQty - ci.getQuantity();
                product.setQuantity(nextQty);
                if (nextQty == 0) {
                    product.setStatus("OUT_OF_STOCK");
                }
                productRepository.save(product);

                orderItemRepository.save(oi);

                BigDecimal lineTotal = ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
                infoTotal = infoTotal.add(lineTotal);
            }

            savedOrder.setTotalAmount(infoTotal);
            savedOrder.setStockDeducted(true);
            createdOrders.add(orderRepository.save(savedOrder));
        }

        // Clear cart
        cartItemRepository.deleteByCartId(cart.getId());

        return createdOrders;
    }

    @Transactional
    public Order cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("Unauthorized: This order does not belong to you");
        }

        // Validate Status
        // Allowed: PLACED, ACCEPTED
        // Denied: OUT_FOR_DELIVERY, DELIVERED, CANCELLED
        String s = order.getStatus();
        if ("WAITING_FOR_FARMER_APPROVAL".equals(s)) s = OrderStatus.PLACED.name();
        if ("FARMER_APPROVED".equals(s)) s = OrderStatus.ACCEPTED.name();
        if ("REJECTED".equals(s)) s = OrderStatus.CANCELLED.name();
        if ("OUT_FOR_DELIVERY".equals(s) || "DELIVERED".equals(s) || "CANCELLED".equals(s)) {
            throw new RuntimeException("Order cannot be cancelled in current status: " + s);
        }

        if (Boolean.TRUE.equals(order.getStockDeducted())) {
            restoreStockForOrder(order);
        }
        order.setStatus(OrderStatus.CANCELLED.name());
        // TODO: Log refund if payment was made?

        return orderRepository.save(order);
    }
}
