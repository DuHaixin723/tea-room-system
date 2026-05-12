package com.tea.management.repository;

import com.tea.management.domain.entity.TeaRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * TeaRoomTypeRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface TeaRoomTypeRepository extends JpaRepository<TeaRoomType, Long> {
}
