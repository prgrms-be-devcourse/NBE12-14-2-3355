package com.gamelog.nbe121423355.domain.user.repository;

import com.gamelog.nbe121423355.domain.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByFamilyId(String familyId);

    // 재발급용: 세션(RefreshToken)과 User를 한 번에 조회 (쿼리 1번)
    @Query("select r from RefreshToken r join fetch r.user where r.familyId = :familyId")
    Optional<RefreshToken> findByFamilyIdWithUser(@Param("familyId") String familyId);

    // Rotation: 현재 jti가 일치할 때만 새 jti로 교체 (원자적 선점)
    // 반환값 1 = 교체 성공, 0 = 이미 다른 요청이 교체했거나 세션이 없음
    // SET 순서 주의: MySQL은 SET을 왼쪽부터 적용하고 앞에서 바뀐 값을 뒤에서 사용
    // → prevJti = currentJti를 먼저 써야 prevJti에 이전 jti가 남음 (순서가 반대면 새 jti가 들어감)
    // clearAutomatically: 교체 후 조회 시 1차 캐시의 이전 상태가 아니라 DB의 최신 상태를 읽도록
    @Modifying(clearAutomatically = true)
    @Query("update RefreshToken r set r.prevJti = r.currentJti, r.currentJti = :newJti, r.rotatedAt = :now " +
            "where r.familyId = :familyId and r.currentJti = :jti")
    int rotate(@Param("familyId") String familyId,
               @Param("jti") String jti,
               @Param("newJti") String newJti,
               @Param("now") LocalDateTime now);

    // 해당 세션 폐기 (로그아웃, 재사용 감지)
    @Modifying
    @Query("delete from RefreshToken r where r.familyId = :familyId")
    void deleteByFamilyId(@Param("familyId") String familyId);

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
