package com.tea.management.repository;

import com.tea.management.domain.entity.EquipmentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * EquipmentReportRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface EquipmentReportRepository extends JpaRepository<EquipmentReport, Long> {

    List<EquipmentReport> findByTeaRoomIdIn(List<Long> teaRoomIds);

    Page<EquipmentReport> findByTeaRoomIdIn(List<Long> teaRoomIds, Pageable pageable);
}
