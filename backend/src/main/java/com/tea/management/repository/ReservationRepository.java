package com.tea.management.repository;

import com.tea.management.domain.entity.Reservation;
import com.tea.management.domain.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
/**
 * ReservationRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByTeaRoomIdIn(List<Long> teaRoomIds);

    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    Page<Reservation> findByTeaRoomIdIn(List<Long> teaRoomIds, Pageable pageable);

    boolean existsByUserIdAndTeaRoomIdAndStatusNot(Long userId, Long teaRoomId, ReservationStatus status);

    List<Reservation> findByTeaRoomIdAndStatusNotInAndReservedEndTimeAfterAndReservedStartTimeBefore(
            Long teaRoomId,
            List<ReservationStatus> statuses,
            LocalDateTime reservedEndTimeAfter,
            LocalDateTime reservedStartTimeBefore
    );

    List<Reservation> findByStatusInAndReservedEndTimeBefore(List<ReservationStatus> statuses, LocalDateTime reservedEndTimeBefore);
}
