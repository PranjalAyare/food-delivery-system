package com.fooddelivery.payment_service.repository;

import com.fooddelivery.payment_service.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// @Repository annotation is technically optional for interfaces extending JpaRepository,
// but it clearly indicates the component's role.
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Spring Data JPA automatically provides CRUD methods (save, findById, findAll, deleteById, etc.)
    // You can add custom query methods here if needed, e.g.:
    // Optional<Payment> findByOrderId(Long orderId);
    // List<Payment> findByStatus(String status);
}
