package com.tea.management.repository;

import com.tea.management.domain.entity.Tea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
/**
 * TeaRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface TeaRepository extends JpaRepository<Tea, Long> {

    @Query("""
            select t from Tea t
            where (:keyword is null or lower(t.name) like lower(concat('%', :keyword, '%'))
                or lower(t.category) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(t.description, '')) like lower(concat('%', :keyword, '%')))
              and (:enabled is null or t.enabled = :enabled)
            """)
    Page<Tea> search(@Param("keyword") String keyword, @Param("enabled") Boolean enabled, Pageable pageable);
}
