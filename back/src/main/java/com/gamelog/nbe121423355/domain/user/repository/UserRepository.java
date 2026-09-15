package com.gamelog.nbe121423355.domain.user.repository;


import com.gamelog.nbe121423355.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email); // 로그인 시 이메일로 유저 조회 쿼리
    Optional<User> existsByEmail(String email); // 회원가입 시 이메일 중복체크용 쿼리
}
