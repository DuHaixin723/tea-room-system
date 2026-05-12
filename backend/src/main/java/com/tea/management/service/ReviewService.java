package com.tea.management.service;

import com.tea.management.domain.entity.Review;
import com.tea.management.domain.entity.Reservation;
import com.tea.management.domain.entity.TeaRoom;
import com.tea.management.domain.enums.OrderStatus;
import com.tea.management.domain.enums.ReservationStatus;
import com.tea.management.dto.request.ReviewRequest;
import com.tea.management.exception.BusinessException;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.repository.ProductOrderRepository;
import com.tea.management.repository.ReservationRepository;
import com.tea.management.repository.ReviewRepository;
import com.tea.management.repository.TeaRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
/**
 * ReviewService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TeaRoomRepository teaRoomRepository;
    private final ReservationRepository reservationRepository;
    private final ProductOrderRepository productOrderRepository;

    public Review create(ReviewRequest request) {
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new ResourceNotFoundException("关联预约不存在"));
        if (!request.userId().equals(reservation.getUserId())) {
            throw new BusinessException("只能评价自己的预约订单");
        }
        if (!request.teaRoomId().equals(reservation.getTeaRoomId())) {
            throw new BusinessException("评价茶室与预约记录不匹配");
        }
        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new BusinessException("仅已完成的预约支持评价");
        }
        if (!productOrderRepository.existsByReservationIdAndStatus(reservation.getId(), OrderStatus.COMPLETED)) {
            throw new BusinessException("订单完成后才能评价");
        }
        if (reviewRepository.existsByReservationId(reservation.getId())) {
            throw new BusinessException("该预约已评价");
        }

        Review review = new Review();
        review.setUserId(request.userId());
        review.setTeaRoomId(request.teaRoomId());
        review.setReservationId(request.reservationId());
        review.setRating(request.rating());
        review.setContent(request.content() == null ? null : request.content().trim());
        return reviewRepository.save(review);
    }

    public Page<Review> listAll(Pageable pageable) {
        return reviewRepository.findAll(pageable);
    }

    public Page<Review> listByUser(Long userId, Pageable pageable) {
        return reviewRepository.findByUserId(userId, pageable);
    }

    public Page<Review> listByStaff(Long staffUserId, Pageable pageable) {
        List<Long> teaRoomIds = teaRoomRepository.findByStaffUserId(staffUserId).stream().map(TeaRoom::getId).toList();
        if (teaRoomIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return reviewRepository.findByTeaRoomIdIn(teaRoomIds, pageable);
    }
}
