package com.tea.management.repository;

import com.tea.management.domain.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * FavoriteRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserId(Long userId);

    Page<Favorite> findByUserId(Long userId, Pageable pageable);

    java.util.Optional<Favorite> findByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, String targetType);
}
