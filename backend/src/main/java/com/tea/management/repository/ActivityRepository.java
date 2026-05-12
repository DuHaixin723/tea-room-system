package com.tea.management.repository;

import com.tea.management.domain.entity.Activity;
import com.tea.management.domain.enums.ActivityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
/**
 * ActivityRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByStatus(ActivityStatus status, Pageable pageable);

    Page<Activity> findByStatusAndEndTimeAfter(ActivityStatus status, LocalDateTime endTime, Pageable pageable);

    List<Activity> findByStatusAndEndTimeBefore(ActivityStatus status, LocalDateTime endTime);
}
