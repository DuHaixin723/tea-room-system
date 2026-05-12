package com.tea.management.service;

import com.tea.management.domain.entity.TeaRoom;
import com.tea.management.domain.entity.TeaRoomType;
import com.tea.management.domain.enums.TeaRoomPricingMode;
import com.tea.management.dto.request.TeaRoomRequest;
import com.tea.management.dto.request.TeaRoomTypeRequest;
import com.tea.management.exception.BusinessException;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.repository.TeaRoomRepository;
import com.tea.management.repository.TeaRoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
/**
 * TeaRoomService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class TeaRoomService {

    private final TeaRoomTypeRepository teaRoomTypeRepository;
    private final TeaRoomRepository teaRoomRepository;

    public Page<TeaRoomType> listTypes(Pageable pageable) {
        return teaRoomTypeRepository.findAll(pageable);
    }

    public TeaRoomType createType(TeaRoomTypeRequest request) {
        validateTypeCapacityRange(request.minCapacity(), request.maxCapacity());

        TeaRoomType type = new TeaRoomType();
        applyType(type, request);
        return teaRoomTypeRepository.save(type);
    }

    public TeaRoomType updateType(Long id, TeaRoomTypeRequest request) {
        validateTypeCapacityRange(request.minCapacity(), request.maxCapacity());
        validateExistingRoomsForType(id, request.minCapacity(), request.maxCapacity());

        TeaRoomType type = teaRoomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("茶室类型不存在"));
        applyType(type, request);
        return teaRoomTypeRepository.save(type);
    }

    public Page<TeaRoom> listRooms(Pageable pageable) {
        return teaRoomRepository.findAll(pageable);
    }

    public Page<TeaRoom> listRoomsByStaff(Long staffUserId, Pageable pageable) {
        return teaRoomRepository.findByStaffUserId(staffUserId, pageable);
    }

    public TeaRoom createRoom(TeaRoomRequest request) {
        TeaRoom room = new TeaRoom();
        applyRoom(room, request);
        return teaRoomRepository.save(room);
    }

    public TeaRoom updateRoom(Long id, TeaRoomRequest request) {
        TeaRoom room = teaRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("茶室不存在"));
        applyRoom(room, request);
        return teaRoomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        teaRoomRepository.deleteById(id);
    }

    private void applyType(TeaRoomType type, TeaRoomTypeRequest request) {
        type.setName(request.name());
        type.setDescription(request.description());
        type.setBasePrice(request.basePrice());
        type.setPricingMode(request.pricingMode() == null || request.pricingMode().isBlank()
                ? TeaRoomPricingMode.PER_ROOM
                : TeaRoomPricingMode.valueOf(request.pricingMode().toUpperCase()));
        type.setMinCapacity(request.minCapacity());
        type.setMaxCapacity(request.maxCapacity());
    }

    private void applyRoom(TeaRoom room, TeaRoomRequest request) {
        TeaRoomType type = teaRoomTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new ResourceNotFoundException("茶室类型不存在"));
        validateRoomCapacity(type, request.capacity());
        validateBusinessHours(request.businessStartTime(), request.businessEndTime());

        room.setTypeId(request.typeId());
        room.setStaffUserId(request.staffUserId());
        room.setName(request.name());
        room.setCapacity(request.capacity());
        room.setEnabled(request.enabled());
        room.setLocation(request.location());
        room.setImageUrl(request.imageUrl());
        room.setBusinessStartTime(request.businessStartTime());
        room.setBusinessEndTime(request.businessEndTime());
        room.setDescription(request.description());
    }

    private void validateTypeCapacityRange(Integer minCapacity, Integer maxCapacity) {
        if (minCapacity == null || maxCapacity == null || minCapacity > maxCapacity) {
            throw new BusinessException("茶室类型的人数范围不合法");
        }
    }

    private void validateExistingRoomsForType(Long typeId, Integer minCapacity, Integer maxCapacity) {
        boolean hasOutOfRangeRooms = teaRoomRepository.findByTypeId(typeId).stream()
                .anyMatch(room -> room.getCapacity() != null && (room.getCapacity() < minCapacity || room.getCapacity() > maxCapacity));
        if (hasOutOfRangeRooms) {
            throw new BusinessException("当前类型下已有茶室容量不在新的人数范围内，请先调整茶室容量");
        }
    }

    private void validateRoomCapacity(TeaRoomType type, Integer capacity) {
        if (capacity == null) {
            throw new BusinessException("茶室容量不能为空");
        }
        if (capacity < type.getMinCapacity() || capacity > type.getMaxCapacity()) {
            throw new BusinessException("茶室容量必须落在所选类型的人数范围内");
        }
    }

    private void validateBusinessHours(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new BusinessException("营业开始和结束时间不能为空");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException("营业结束时间必须晚于开始时间");
        }
    }
}
