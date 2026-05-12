package com.tea.management.repository;

import com.tea.management.domain.entity.ActivityRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * ActivityRegistrationRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface ActivityRegistrationRepository extends JpaRepository<ActivityRegistration, Long> {

    List<ActivityRegistration> findByUserId(Long userId);

    List<ActivityRegistration> findByActivityId(Long activityId);

    Page<ActivityRegistration> findByUserId(Long userId, Pageable pageable);
}
