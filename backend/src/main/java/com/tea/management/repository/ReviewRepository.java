package com.tea.management.repository;

import com.tea.management.domain.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * ReviewRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByUserId(Long userId);

    Page<Review> findByUserId(Long userId, Pageable pageable);

    List<Review> findByTeaRoomIdIn(List<Long> teaRoomIds);

    Page<Review> findByTeaRoomIdIn(List<Long> teaRoomIds, Pageable pageable);

    boolean existsByReservationId(Long reservationId);
}
