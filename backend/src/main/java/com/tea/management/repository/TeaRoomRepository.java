package com.tea.management.repository;

import com.tea.management.domain.entity.TeaRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * TeaRoomRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface TeaRoomRepository extends JpaRepository<TeaRoom, Long> {

    List<TeaRoom> findByTypeId(Long typeId);

    List<TeaRoom> findByStaffUserId(Long staffUserId);

    Page<TeaRoom> findByStaffUserId(Long staffUserId, Pageable pageable);

    boolean existsByIdAndStaffUserId(Long id, Long staffUserId);
}
