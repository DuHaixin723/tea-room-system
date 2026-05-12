package com.tea.management.service;

import com.tea.management.domain.entity.Favorite;
import com.tea.management.domain.entity.OrderItem;
import com.tea.management.domain.entity.ProductOrder;
import com.tea.management.domain.entity.Reservation;
import com.tea.management.domain.entity.Review;
import com.tea.management.domain.entity.Tea;
import com.tea.management.domain.entity.TeaRoom;
import com.tea.management.dto.response.RecommendationResponse;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.repository.FavoriteRepository;
import com.tea.management.repository.OrderItemRepository;
import com.tea.management.repository.ProductOrderRepository;
import com.tea.management.repository.ReservationRepository;
import com.tea.management.repository.ReviewRepository;
import com.tea.management.repository.TeaRepository;
import com.tea.management.repository.TeaRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
/**
 * RecommendationService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final String FAVORITE_TEA = "TEA";
    private static final String FAVORITE_TEA_ROOM = "TEA_ROOM";

    private final TeaRepository teaRepository;
    private final TeaRoomRepository teaRoomRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReservationRepository reservationRepository;
    private final ProductOrderRepository productOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;

    public RecommendationResponse recommend(Long userId) {
        return recommend(userId, null);
    }

    public RecommendationResponse recommend(Long userId, Long reservationId) {
        List<Tea> enabledTeas = teaRepository.findAll().stream()
                .filter(tea -> Boolean.TRUE.equals(tea.getEnabled()))
                .toList();
        List<TeaRoom> enabledRooms = teaRoomRepository.findAll().stream()
                .filter(room -> Boolean.TRUE.equals(room.getEnabled()))
                .toList();

        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        List<Reservation> reservations = reservationRepository.findByUserId(userId);
        List<ProductOrder> orders = productOrderRepository.findByUserId(userId);
        List<Review> reviews = reviewRepository.findByUserId(userId);

        Map<Long, Tea> teaById = enabledTeas.stream().collect(Collectors.toMap(Tea::getId, tea -> tea));
        Map<Long, TeaRoom> roomById = enabledRooms.stream().collect(Collectors.toMap(TeaRoom::getId, room -> room));
        Map<Long, Reservation> reservationById = reservations.stream()
                .collect(Collectors.toMap(Reservation::getId, reservation -> reservation));

        List<OrderItem> orderItems = orders.stream()
                .flatMap(order -> orderItemRepository.findByOrderId(order.getId()).stream())
                .toList();
        Map<Long, List<OrderItem>> orderItemsByOrderId = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        Set<Long> favoriteTeaIds = favorites.stream()
                .filter(favorite -> FAVORITE_TEA.equalsIgnoreCase(favorite.getTargetType()))
                .map(Favorite::getTargetId)
                .collect(Collectors.toSet());
        Set<Long> favoriteRoomIds = favorites.stream()
                .filter(favorite -> FAVORITE_TEA_ROOM.equalsIgnoreCase(favorite.getTargetType()))
                .map(Favorite::getTargetId)
                .collect(Collectors.toSet());

        Map<String, Double> categoryPreference = new HashMap<>();
        Map<Long, Double> teaDirectPreference = new HashMap<>();
        Map<Long, Map<Long, Double>> roomTeaPreference = new HashMap<>();
        Map<Long, Map<Long, Double>> roomTypeTeaPreference = new HashMap<>();
        Map<String, Map<Long, Double>> locationTeaPreference = new HashMap<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        int priceSamples = 0;

        for (Long teaId : favoriteTeaIds) {
            Tea tea = teaById.get(teaId);
            if (tea == null) {
                continue;
            }
            teaDirectPreference.merge(teaId, 8D, Double::sum);
            categoryPreference.merge(tea.getCategory(), 5D, Double::sum);
            totalPrice = totalPrice.add(tea.getPrice());
            priceSamples++;
        }

        for (OrderItem item : orderItems) {
            Tea tea = teaById.get(item.getTeaId());
            if (tea == null) {
                continue;
            }
            double quantityScore = Math.max(1, item.getQuantity()) * 3D;
            teaDirectPreference.merge(item.getTeaId(), quantityScore, Double::sum);
            categoryPreference.merge(tea.getCategory(), quantityScore * 0.85D, Double::sum);
            totalPrice = totalPrice.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            priceSamples += item.getQuantity();
        }

        for (ProductOrder order : orders) {
            if (order.getReservationId() == null) {
                continue;
            }
            Reservation linkedReservation = reservationById.get(order.getReservationId());
            if (linkedReservation == null) {
                continue;
            }
            TeaRoom linkedRoom = roomById.get(linkedReservation.getTeaRoomId());
            if (linkedRoom == null) {
                continue;
            }
            for (OrderItem item : orderItemsByOrderId.getOrDefault(order.getId(), List.of())) {
                double contextScore = Math.max(1, item.getQuantity()) * 2.6D;
                roomTeaPreference.computeIfAbsent(linkedRoom.getId(), key -> new HashMap<>())
                        .merge(item.getTeaId(), contextScore, Double::sum);
                roomTypeTeaPreference.computeIfAbsent(linkedRoom.getTypeId(), key -> new HashMap<>())
                        .merge(item.getTeaId(), contextScore * 0.8D, Double::sum);
                if (linkedRoom.getLocation() != null && !linkedRoom.getLocation().isBlank()) {
                    locationTeaPreference.computeIfAbsent(linkedRoom.getLocation(), key -> new HashMap<>())
                            .merge(item.getTeaId(), contextScore * 0.5D, Double::sum);
                }
            }
        }

        BigDecimal averagePrice = priceSamples == 0
                ? enabledTeas.stream().map(Tea::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(enabledTeas.size(), 1)), 2, RoundingMode.HALF_UP)
                : totalPrice.divide(BigDecimal.valueOf(priceSamples), 2, RoundingMode.HALF_UP);

        Map<Long, Double> roomDirectPreference = new HashMap<>();
        Map<Long, Double> roomTypePreference = new HashMap<>();
        Map<String, Double> locationPreference = new HashMap<>();
        double capacityTotal = 0D;
        int capacitySamples = 0;

        for (Long roomId : favoriteRoomIds) {
            TeaRoom room = roomById.get(roomId);
            if (room == null) {
                continue;
            }
            roomDirectPreference.merge(roomId, 9D, Double::sum);
            roomTypePreference.merge(room.getTypeId(), 5D, Double::sum);
            if (room.getLocation() != null && !room.getLocation().isBlank()) {
                locationPreference.merge(room.getLocation(), 4D, Double::sum);
            }
            capacityTotal += room.getCapacity();
            capacitySamples++;
        }

        for (Reservation reservation : reservations) {
            TeaRoom room = roomById.get(reservation.getTeaRoomId());
            if (room == null) {
                continue;
            }
            double reservationScore = switch (reservation.getStatus()) {
                case COMPLETED -> 7D;
                case CHARGING, STAFF_CONFIRMED_CHECK_IN -> 6D;
                case USER_CHECKED_IN, CONFIRMED -> 5D;
                case PENDING -> 3D;
                case CANCELLED, NO_SHOW -> 1D;
            };
            roomDirectPreference.merge(room.getId(), reservationScore, Double::sum);
            roomTypePreference.merge(room.getTypeId(), reservationScore * 0.75D, Double::sum);
            if (room.getLocation() != null && !room.getLocation().isBlank()) {
                locationPreference.merge(room.getLocation(), reservationScore * 0.65D, Double::sum);
            }
            capacityTotal += room.getCapacity();
            capacitySamples++;
        }

        for (Review review : reviews) {
            TeaRoom room = roomById.get(review.getTeaRoomId());
            if (room == null) {
                continue;
            }
            double reviewScore = Math.max(1, review.getRating()) * 1.6D;
            roomDirectPreference.merge(room.getId(), reviewScore, Double::sum);
            roomTypePreference.merge(room.getTypeId(), reviewScore * 0.7D, Double::sum);
        }

        double averageCapacity = capacitySamples == 0
                ? enabledRooms.stream().mapToInt(TeaRoom::getCapacity).average().orElse(6D)
                : capacityTotal / capacitySamples;

        Reservation selectedReservation = reservationId == null ? null : reservationById.get(reservationId);
        TeaRoom selectedRoom = selectedReservation == null ? null : roomById.get(selectedReservation.getTeaRoomId());

        List<Tea> recommendedTeas = enabledTeas.stream()
                .sorted(descScoreComparator(
                        tea -> teaScore(
                                tea,
                                teaDirectPreference,
                                categoryPreference,
                                averagePrice,
                                selectedRoom,
                                roomTeaPreference,
                                roomTypeTeaPreference,
                                locationTeaPreference
                        ),
                        Tea::getId
                ))
                .limit(4)
                .toList();

        List<TeaRoom> recommendedRooms = enabledRooms.stream()
                .sorted(descScoreComparator(room -> roomScore(room, roomDirectPreference, roomTypePreference, locationPreference, averageCapacity), TeaRoom::getId))
                .limit(4)
                .toList();

        return new RecommendationResponse(
                recommendedTeas.stream().map(ResponseMapper::toTeaResponse).toList(),
                recommendedRooms.stream().map(ResponseMapper::toTeaRoomResponse).toList()
        );
    }

    private <T> Comparator<T> descScoreComparator(ToDoubleFunction<T> scorer, java.util.function.Function<T, Long> idGetter) {
        return Comparator.comparingDouble(scorer).reversed()
                .thenComparing(idGetter);
    }

    private double teaScore(Tea tea,
                            Map<Long, Double> directPreference,
                            Map<String, Double> categoryPreference,
                            BigDecimal averagePrice,
                            TeaRoom selectedRoom,
                            Map<Long, Map<Long, Double>> roomTeaPreference,
                            Map<Long, Map<Long, Double>> roomTypeTeaPreference,
                            Map<String, Map<Long, Double>> locationTeaPreference) {
        double score = 1D;
        score += directPreference.getOrDefault(tea.getId(), 0D);
        score += categoryPreference.getOrDefault(tea.getCategory(), 0D);

        double priceGap = Math.abs(tea.getPrice().subtract(averagePrice).doubleValue());
        score += Math.max(0D, 6D - (priceGap / 12D));
        score += Math.min(tea.getStock(), 200) / 80D;

        if (selectedRoom != null) {
            score += roomTeaPreference.getOrDefault(selectedRoom.getId(), Map.of()).getOrDefault(tea.getId(), 0D);
            score += roomTypeTeaPreference.getOrDefault(selectedRoom.getTypeId(), Map.of()).getOrDefault(tea.getId(), 0D);
            if (selectedRoom.getLocation() != null && !selectedRoom.getLocation().isBlank()) {
                score += locationTeaPreference.getOrDefault(selectedRoom.getLocation(), Map.of()).getOrDefault(tea.getId(), 0D);
            }
        }

        return score;
    }

    private double roomScore(TeaRoom room,
                             Map<Long, Double> directPreference,
                             Map<Long, Double> typePreference,
                             Map<String, Double> locationPreference,
                             double averageCapacity) {
        double score = 1D;
        score += directPreference.getOrDefault(room.getId(), 0D);
        score += typePreference.getOrDefault(room.getTypeId(), 0D);
        score += locationPreference.getOrDefault(room.getLocation(), 0D);

        double capacityGap = Math.abs(room.getCapacity() - averageCapacity);
        score += Math.max(0D, 5D - (capacityGap / 2D));

        if (room.getDescription() != null && !room.getDescription().isBlank()) {
            score += 0.8D;
        }
        return score;
    }
}
