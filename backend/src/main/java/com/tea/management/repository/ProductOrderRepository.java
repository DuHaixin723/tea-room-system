package com.tea.management.repository;

import com.tea.management.domain.entity.ProductOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
/**
 * ProductOrderRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long> {

    List<ProductOrder> findByUserId(Long userId);

    Optional<ProductOrder> findByOrderNo(String orderNo);

    Page<ProductOrder> findByUserId(Long userId, Pageable pageable);

    Page<ProductOrder> findByReservationIdIn(List<Long> reservationIds, Pageable pageable);

    List<ProductOrder> findByReservationId(Long reservationId);

    boolean existsByReservationIdAndStatus(Long reservationId, com.tea.management.domain.enums.OrderStatus status);

    List<ProductOrder> findByStatusInAndReservationIdIsNotNull(List<com.tea.management.domain.enums.OrderStatus> statuses);
}
