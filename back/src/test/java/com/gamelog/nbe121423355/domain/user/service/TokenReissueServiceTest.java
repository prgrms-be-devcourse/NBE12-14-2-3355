package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.dto.LoginRequestDto;
import com.gamelog.nbe121423355.domain.user.entity.RefreshToken;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.RefreshTokenRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.global.security.jwt.JwtProperties;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TokenReissueServiceTest {

    @Autowired
    private TokenReissueService tokenReissueService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EntityManager entityManager;

    private User saveUser(String email, String nickname) {
        return userRepository.save(new User(nickname, email, passwordEncoder.encode("password123")));
    }

    private String loginAndGetRefreshToken(String email) {
        return userService.login(new LoginRequestDto(email, "password123")).refreshToken();
    }

    @Test
    @DisplayName("재발급 성공 - expectedUserId가 refreshToken의 사용자와 같으면 해당 사용자의 accessToken 발급")
    void reissue_success_userIdMatch() {
        User user = saveUser("test@test.com", "nickname");
        String refreshToken = loginAndGetRefreshToken("test@test.com");

        TokenReissueService.ReissueResult result = tokenReissueService.reissue(refreshToken, user.getId());

        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.role()).isEqualTo(user.getRole());
        assertThat(jwtProvider.getUserId(result.accessToken())).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("재발급 성공 - expectedUserId 없이(/refresh API) 호출하면 비교 없이 발급")
    void reissue_success_withoutExpectedUserId() {
        User user = saveUser("test@test.com", "nickname");
        String refreshToken = loginAndGetRefreshToken("test@test.com");

        TokenReissueService.ReissueResult result = tokenReissueService.reissue(refreshToken);

        assertThat(result.userId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("재발급 실패 - expectedUserId가 refreshToken의 사용자와 다르면 401-2")
    void reissue_fail_userIdMismatch() {
        User userA = saveUser("a@test.com", "userA");
        saveUser("b@test.com", "userB");
        String refreshTokenOfB = loginAndGetRefreshToken("b@test.com");

        assertThatThrownBy(() -> tokenReissueService.reissue(refreshTokenOfB, userA.getId()))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("401-2"));
    }

    // ===== Rotation =====
    // H2는 MySQL(REPEATABLE READ)의 스냅샷·잠금 동작과 UPDATE SET 적용 순서를 그대로 재현하지 못하므로
    // 여기서는 교체/유예/재사용 분기 로직만 검증 (동시 요청 경쟁은 MySQL 환경에서 별도 확인 필요)

    private String jtiOf(String refreshToken) {
        return jwtProvider.parseClaims(refreshToken).getId();
    }

    private String familyIdOf(String refreshToken) {
        return jwtProvider.getFamilyId(jwtProvider.parseClaims(refreshToken));
    }

    // 마지막 교체 시각을 과거로 돌려 유예 시간이 지난 상황을 만든다
    private void moveRotatedAtBack(String familyId, Duration duration) {
        entityManager.createQuery("update RefreshToken r set r.rotatedAt = :rotatedAt where r.familyId = :familyId")
                .setParameter("rotatedAt", LocalDateTime.now().minus(duration))
                .setParameter("familyId", familyId)
                .executeUpdate();
        entityManager.clear();
    }

    private void assertResultCode(Throwable e, String resultCode) {
        assertThat(e).isInstanceOf(ServiceException.class);
        assertThat(((ServiceException) e).getResultCode()).isEqualTo(resultCode);
    }

    @Test
    @DisplayName("Rotation - 재발급하면 같은 세션(fid)에 새 jti의 refreshToken이 발급되고, DB의 current/prev jti가 교체됨")
    void rotation_issuesNewRefreshToken() {
        saveUser("test@test.com", "nickname");
        String oldRefreshToken = loginAndGetRefreshToken("test@test.com");

        TokenReissueService.ReissueResult result = tokenReissueService.reissue(oldRefreshToken);

        assertThat(familyIdOf(result.refreshToken())).isEqualTo(familyIdOf(oldRefreshToken));
        assertThat(jtiOf(result.refreshToken())).isNotEqualTo(jtiOf(oldRefreshToken));
        RefreshToken session = refreshTokenRepository.findByFamilyId(familyIdOf(oldRefreshToken)).orElseThrow();
        assertThat(session.getCurrentJti()).isEqualTo(jtiOf(result.refreshToken()));
        assertThat(session.getPrevJti()).isEqualTo(jtiOf(oldRefreshToken));
        assertThat(session.getRotatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Rotation - 교체돼도 세션 만료 시각(로그인 기준 절대 만료)은 그대로 유지")
    void rotation_keepsAbsoluteExpiry() {
        saveUser("test@test.com", "nickname");
        String oldRefreshToken = loginAndGetRefreshToken("test@test.com");
        Date oldExpiration = jwtProvider.parseClaims(oldRefreshToken).getExpiration();

        TokenReissueService.ReissueResult result = tokenReissueService.reissue(oldRefreshToken);

        assertThat(jwtProvider.parseClaims(result.refreshToken()).getExpiration()).isEqualTo(oldExpiration);
        assertThat(result.refreshTokenExpiryDate())
                .isEqualTo(refreshTokenRepository.findByFamilyId(familyIdOf(oldRefreshToken)).orElseThrow().getExpiryDate());
    }

    @Test
    @DisplayName("Rotation - 새로 받은 refreshToken으로 다시 재발급 가능 (연속 교체)")
    void rotation_newRefreshTokenWorks() {
        saveUser("test@test.com", "nickname");
        String refreshToken1 = loginAndGetRefreshToken("test@test.com");
        String refreshToken2 = tokenReissueService.reissue(refreshToken1).refreshToken();

        String refreshToken3 = tokenReissueService.reissue(refreshToken2).refreshToken();

        assertThat(jtiOf(refreshToken3)).isNotEqualTo(jtiOf(refreshToken2));
        assertThat(familyIdOf(refreshToken3)).isEqualTo(familyIdOf(refreshToken1));
    }

    @Test
    @DisplayName("유예 시간 - 교체 직후 이전 refreshToken으로 다시 요청하면 성공하고, 이미 교체된 같은 refreshToken을 다시 받음")
    void gracePeriod_returnsSameRefreshToken() {
        saveUser("test@test.com", "nickname");
        String oldRefreshToken = loginAndGetRefreshToken("test@test.com");
        TokenReissueService.ReissueResult first = tokenReissueService.reissue(oldRefreshToken);

        TokenReissueService.ReissueResult second = tokenReissueService.reissue(oldRefreshToken);

        assertThat(jtiOf(second.refreshToken())).isEqualTo(jtiOf(first.refreshToken()));
        assertThat(second.accessToken()).isNotBlank();
        // 유예 요청은 교체를 다시 하지 않음
        RefreshToken session = refreshTokenRepository.findByFamilyId(familyIdOf(oldRefreshToken)).orElseThrow();
        assertThat(session.getCurrentJti()).isEqualTo(jtiOf(first.refreshToken()));
        assertThat(session.getPrevJti()).isEqualTo(jtiOf(oldRefreshToken));
    }

    @Test
    @DisplayName("재사용 감지 - 유예 시간이 지난 뒤 이전 refreshToken을 쓰면 401-3, 세션이 삭제되어 최신 refreshToken도 무효")
    void reuseDetected_afterGracePeriod_revokesSession() {
        saveUser("test@test.com", "nickname");
        String oldRefreshToken = loginAndGetRefreshToken("test@test.com");
        String newRefreshToken = tokenReissueService.reissue(oldRefreshToken).refreshToken();
        String familyId = familyIdOf(oldRefreshToken);
        moveRotatedAtBack(familyId, Duration.ofSeconds(11));

        assertThatThrownBy(() -> tokenReissueService.reissue(oldRefreshToken))
                .satisfies(e -> assertResultCode(e, "401-3"));

        // 예외를 던져도 세션 삭제는 롤백되지 않아야 함 (noRollbackFor)
        assertThat(refreshTokenRepository.findByFamilyId(familyId)).isEmpty();
        assertThatThrownBy(() -> tokenReissueService.reissue(newRefreshToken))
                .satisfies(e -> assertResultCode(e, "401-2"));
    }

    @Test
    @DisplayName("재사용 감지 - 두 세대 이전 refreshToken은 유예 시간 안이어도 재사용으로 판단 (유예는 직전 1세대만)")
    void reuseDetected_twoGenerationsOld() {
        saveUser("test@test.com", "nickname");
        String refreshToken1 = loginAndGetRefreshToken("test@test.com");
        String refreshToken2 = tokenReissueService.reissue(refreshToken1).refreshToken();
        tokenReissueService.reissue(refreshToken2);

        assertThatThrownBy(() -> tokenReissueService.reissue(refreshToken1))
                .satisfies(e -> assertResultCode(e, "401-3"));
        assertThat(refreshTokenRepository.findByFamilyId(familyIdOf(refreshToken1))).isEmpty();
    }

    @Test
    @DisplayName("재사용 감지 - 다른 기기(다른 세션)는 영향 없음")
    void reuseDetected_otherSessionUnaffected() {
        saveUser("test@test.com", "nickname");
        String deviceA = loginAndGetRefreshToken("test@test.com");
        String deviceB = loginAndGetRefreshToken("test@test.com");
        tokenReissueService.reissue(deviceA);
        moveRotatedAtBack(familyIdOf(deviceA), Duration.ofSeconds(11));

        assertThatThrownBy(() -> tokenReissueService.reissue(deviceA))
                .satisfies(e -> assertResultCode(e, "401-3"));

        assertThat(tokenReissueService.reissue(deviceB).accessToken()).isNotBlank();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("트랜잭션 적용 - 테스트 트랜잭션 없이 reissue(String)(/refresh 경로)를 호출해도 교체 UPDATE가 실행됨")
    void reissue_withoutTestTransaction_rotates() {
        // 클래스의 @Transactional이 테스트 트랜잭션을 미리 열어두면, 서비스에 트랜잭션이 없어도 UPDATE가 성공해서
        // self-invocation으로 @Transactional이 빠진 버그(TransactionRequiredException)를 잡지 못함 → 이 테스트만 트랜잭션 없이 실행
        // 데이터가 실제로 커밋되므로 끝나면 직접 정리
        User user = saveUser("no-tx@test.com", "noTxUser");
        String familyId = null;
        try {
            String oldRefreshToken = loginAndGetRefreshToken("no-tx@test.com");
            familyId = familyIdOf(oldRefreshToken);

            TokenReissueService.ReissueResult result = tokenReissueService.reissue(oldRefreshToken);

            assertThat(jtiOf(result.refreshToken())).isNotEqualTo(jtiOf(oldRefreshToken));
            assertThat(refreshTokenRepository.findByFamilyId(familyId).orElseThrow().getCurrentJti())
                    .isEqualTo(jtiOf(result.refreshToken()));
        } finally {
            if (familyId != null) {
                refreshTokenRepository.findByFamilyId(familyId).ifPresent(refreshTokenRepository::delete);
            }
            userRepository.delete(user);
        }
    }

    @Test
    @DisplayName("재발급 실패 - fid 클레임이 없는 refreshToken(Rotation 이전 방식)이면 401-2")
    void reissue_fail_noFamilyId() {
        User user = saveUser("test@test.com", "nickname");
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
        String legacyRefreshToken = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .claim("token_type", "Refresh")
                .issuedAt(new Date())
                .expiration(jwtProvider.newRefreshTokenExpiration())
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> tokenReissueService.reissue(legacyRefreshToken))
                .satisfies(e -> assertResultCode(e, "401-2"));
    }
}
