package com.tea.management.repository;

import com.tea.management.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * OrderItemRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}
