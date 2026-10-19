package com.gamelog.nbe121423355.domain.user.repository;

import com.gamelog.nbe121423355.domain.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenId(String tokenId);
    void deleteByTokenId(String tokenId);

    // 재발급용: RefreshToken과 User를 한 번에 조회 (쿼리 1번)
    @Query("select r from RefreshToken r join fetch r.user where r.tokenId = :tokenId")
    Optional<RefreshToken> findByTokenIdWithUser(@Param("tokenId") String tokenId);

    // 해당 유저의 refreshToken 전체 폐기 (벌크 삭제, 쿼리 1번)
    // flushAutomatically: 삭제 전에 같은 트랜잭션의 변경(예: 비밀번호 변경)을 먼저 DB에 반영
    // → 없으면 clearAutomatically가 영속성 컨텍스트를 비우면서 flush 안 된 변경이 버려짐
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RefreshToken r where r.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from RefreshToken r where r.expiryDate < :now")
    int deleteAllExpiredBefore(@Param("now") LocalDateTime now);
}
