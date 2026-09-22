package com.gamelog.nbe121423355.domain.user.repository;


import com.gamelog.nbe121423355.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByNicknameContainingIgnoreCase(String nickname, Pageable pageable);
    Page<User> findByNicknameContainingIgnoreCaseAndIdNot(String nickname, Long excludedUserId, Pageable pageable);
    Optional<User> findByEmail(String email); // 로그인 시 이메일로 유저 조회 쿼리
    boolean existsByEmail(String email); // 회원가입 시 이메일 중복체크용 쿼리
    boolean existsByNickname(String nickname); // 회원가입 시 닉네임 중복체크용 쿼리
}
