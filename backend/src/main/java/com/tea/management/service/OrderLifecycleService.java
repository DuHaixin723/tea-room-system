package com.tea.management.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLifecycleService {

    private final ReservationService reservationService;
    private final OrderService orderService;

    @Scheduled(cron = "0/30 * * * * ?", zone = "Asia/Shanghai")
    public void completeExpiredReservationsAndOrders() {
        int completedReservations = reservationService.completeExpiredReservations();
        int completedOrders = orderService.completeExpiredOrders();
        if (completedReservations > 0 || completedOrders > 0) {
            log.info("Automatically completed {} reservations and {} orders", completedReservations, completedOrders);
        }
    }
}
