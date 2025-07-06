package com.fooddelivery.order_service.repository;

import com.fooddelivery.order_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Add custom queries if needed later
}
