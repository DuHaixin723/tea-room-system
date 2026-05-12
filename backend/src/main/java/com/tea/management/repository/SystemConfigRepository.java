package com.tea.management.repository;

import com.tea.management.domain.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
/**
 * SystemConfigRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    Optional<SystemConfig> findByConfigKey(String configKey);

    boolean existsByConfigKey(String configKey);
}
