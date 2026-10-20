package com.gamelog.nbe121423355.domain.user.repository;


import com.gamelog.nbe121423355.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByNicknameContainingIgnoreCaseAndDeletedAtIsNull(String nickname, Pageable pageable);
    Page<User> findByNicknameContainingIgnoreCaseAndIdNotAndDeletedAtIsNull(String nickname, Long excludedUserId, Pageable pageable);
    Optional<User> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByIdAndDeletedAtIsNull(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}