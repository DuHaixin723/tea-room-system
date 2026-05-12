package com.tea.management.repository;

import com.tea.management.domain.entity.User;
import com.tea.management.domain.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
/**
 * UserRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    List<User> findByRole(RoleType role);

    Page<User> findByRole(RoleType role, Pageable pageable);
}
