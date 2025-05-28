package com.project.Fashion.service;

import com.project.Fashion.model.Order;
import com.project.Fashion.model.Product;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.OrderRepository;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public Order createOrder(Order order) {
        // Optional: Validate associated entities if needed
        if (order.getProduct() != null && order.getProduct().getId() != null) {
            Product product = productRepository.findById(order.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            order.setProduct(product);
        }

        if (order.getUser() != null && order.getUser().getId() != null) {
            User user = userRepository.findById(order.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            order.setUser(user);
        }

        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public Order updateOrder(Long id, Order updatedOrder) {
        Order existing = getOrder(id);

        existing.setProduct(updatedOrder.getProduct());
        existing.setUser(updatedOrder.getUser());
        existing.setQuantity(updatedOrder.getQuantity());
        existing.setDate(updatedOrder.getDate());
        existing.setDelivery(updatedOrder.getDelivery());
        existing.setTotal(updatedOrder.getTotal());

        return orderRepository.save(existing);
    }

    public Order patchOrder(Long id, Map<String, Object> updates) {
        Order order = getOrder(id);

        updates.forEach((key, value) -> {
            switch (key) {
                case "quantity" -> order.setQuantity(Integer.parseInt(value.toString()));
                case "total" -> order.setTotal(Float.parseFloat(value.toString()));
                case "date" -> order.setDate(java.sql.Date.valueOf(value.toString()));
                case "userId" -> {
                    User user = userRepository.findById(value.toString())
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    order.setUser(user);
                }
                case "productId" -> {
                    Product product = productRepository.findById(Long.parseLong(value.toString()))
                            .orElseThrow(() -> new RuntimeException("Product not found"));
                    order.setProduct(product);
                }
                default -> throw new IllegalArgumentException("Invalid field: " + key);
            }
        });

        return orderRepository.save(order);
    }

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found");
        }
        orderRepository.deleteById(id);
    }
}
