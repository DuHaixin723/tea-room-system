package com.tea.management.service;

import com.tea.management.domain.enums.RoleType;
import com.tea.management.dto.response.StatisticResponse;
import com.tea.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
/**
 * StatisticService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class StatisticService {

    private final UserRepository userRepository;
    private final TeaRoomRepository teaRoomRepository;
    private final TeaRepository teaRepository;
    private final ReservationRepository reservationRepository;
    private final ActivityRepository activityRepository;
    private final ProductOrderRepository productOrderRepository;

    public StatisticResponse overview() {
        BigDecimal totalAmount = productOrderRepository.findAll().stream()
                .map(order -> order.getAmount() == null ? BigDecimal.ZERO : order.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StatisticResponse(
                userRepository.findByRole(RoleType.USER).size(),
                userRepository.findByRole(RoleType.STAFF).size(),
                userRepository.findByRole(RoleType.ADMIN).size(),
                teaRoomRepository.count(),
                teaRepository.count(),
                reservationRepository.count(),
                activityRepository.count(),
                productOrderRepository.count(),
                totalAmount
        );
    }
}
