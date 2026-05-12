package com.tea.management.repository;

import com.tea.management.domain.entity.StaffComplaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * StaffComplaintRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface StaffComplaintRepository extends JpaRepository<StaffComplaint, Long> {

    Page<StaffComplaint> findByUserId(Long userId, Pageable pageable);

    Page<StaffComplaint> findByStaffUserId(Long staffUserId, Pageable pageable);
}

