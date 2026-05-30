package com.soa.repositories;

import com.soa.models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Spring Data JPA resolves cart_id via the @ManyToOne cart relationship
    List<OrderItem> findByCartId(Long cartId);
    List<OrderItem> findByCartIdAndStatus(Long cartId, OrderItem.ItemStatus status);
}
