package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.dto.*;
import com.gamelog.nbe121423355.domain.user.entity.RefreshToken;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.RefreshTokenRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
import com.gamelog.nbe121423355.global.upload.ImageUploadService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ImageUploadService imageUploadService;
    private final TokenReissueService tokenReissueService;

    // 로그인 결과를 Controller에 전달하기 위한 내부 운반용 record
    // refreshTokenExpiryDate: 쿠키 maxAge 계산용 (세션의 절대 만료 시각)
    public record LoginResult(UserDto user, String accessToken, String refreshToken, LocalDateTime refreshTokenExpiryDate) {}

    // 새로 만든 세션의 refreshToken과 만료 시각 (비밀번호 변경 시 Controller가 쿠키로 내려줌)
    public record IssuedSession(String refreshToken, LocalDateTime expiryDate) {}

    // 회원가입 메소드
    public UserDto signUp(SignupRequestDto signUpDto) {
        if (userRepository.existsByEmail(signUpDto.email())) {
            throw new ServiceException("409-1", "이미 존재하는 이메일입니다.");
        }
        if (userRepository.existsByNickname(signUpDto.nickname())) {
            throw new ServiceException("409-2", "이미 존재하는 닉네임입니다.");
        }
        String encodePassword = passwordEncoder.encode(signUpDto.password());
        User user = new User(
                signUpDto.nickname(),
                signUpDto.email(),
                encodePassword
        );

        User saveUser = userRepository.save(user);
        return new UserDto(saveUser);
    }

    // 로그인 메소드
    public LoginResult login(LoginRequestDto loginRequestDto) {
        Optional<User> userOptional = userRepository.findByEmail(loginRequestDto.email());
        if(userOptional.isEmpty()) {
            throw new ServiceException("401-1", "이메일 또는 비밀번호가 일치하지 않습니다.");
        }
        User user = userOptional.get();
        if(!passwordEncoder.matches(loginRequestDto.password(), user.getPassword())){
            throw new ServiceException("401-1", "이메일 또는 비밀번호가 일치하지 않습니다.");
        }
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        IssuedSession session = issueSession(user);
        return new LoginResult(new UserDto(user), accessToken, session.refreshToken(), session.expiryDate());
    }

    // 새 로그인 세션 생성 (family 1개 + 첫 refreshToken)
    // 세션 만료 시각은 여기서 정해지고 Rotation 때도 그대로 유지 (절대 만료)
    private IssuedSession issueSession(User user) {
        String familyId = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();
        Date expiration = jwtProvider.newRefreshTokenExpiration();
        LocalDateTime expiryDate = expiration.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        refreshTokenRepository.save(RefreshToken.create(user, familyId, jti, expiryDate));
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), familyId, jti, expiration);
        return new IssuedSession(refreshToken, expiryDate);
    }

    // 로그아웃 메소드 (해당 세션 전체 폐기)
    @Transactional
    public void logout(String refreshToken) {
        if (jwtProvider.validateToken(refreshToken)) {
            String familyId = jwtProvider.getFamilyId(jwtProvider.parseClaims(refreshToken));
            if (familyId != null) {
                refreshTokenRepository.deleteByFamilyId(familyId);
            }
        }
    }

    // 토큰 갱신 메소드 (재발급 로직은 TokenReissueService에서 필터와 공유)
    // 교체된 refreshToken도 쿠키로 내려줘야 하므로 결과 전체를 Controller에 전달
    // 주의: @Transactional을 붙이면 재사용 감지 시 세션 삭제가 바깥 트랜잭션 롤백으로 취소됨
    public TokenReissueService.ReissueResult refresh(String refreshToken) {
        return tokenReissueService.reissue(refreshToken);
    }

    // 내정보 가져오기 - 온보딩 확인 여부
    public UserDto getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 유저 입니다."));
        return new UserDto(user);
    }

    //유저 정보 가져오기
    public PublicUserDto getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(
                        "404-1",
                        "존재하지 않는 유저입니다."
                ));

        return new PublicUserDto(user);
    }

    // 온보딩 완료(선호 정보 저장 후 확정)
    @Transactional
    public UserDto exitOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 유저 입니다."));
        user.completeOnboarding();
        return new UserDto(user);
    }

    // 온보딩 건너뛰기
    @Transactional
    public UserDto skipOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 유저 입니다."));
        user.completeOnboarding();
        return new UserDto(user);
    }

    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean checkNicknameDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequestDto updateProfileRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 유저 입니다."));
        if(!user.getNickname().equals(updateProfileRequestDto.nickname()) && userRepository.existsByNickname(updateProfileRequestDto.nickname())) {
            throw new ServiceException("409-2", "이미 존재하는 닉네임 입니다.");
        }
        String oldProfileImageUrl = user.getProfileImageUrl();
        user.updateProfile(updateProfileRequestDto.nickname(), updateProfileRequestDto.profileImageUrl(), updateProfileRequestDto.bio());
        if(oldProfileImageUrl != null && !oldProfileImageUrl.equals(updateProfileRequestDto.profileImageUrl())) {
            imageUploadService.deleteImage(oldProfileImageUrl);
        }
        return new UserDto(user);
    }

    // 비밀번호 변경(로그인 상태)
    // 모든 기기의 세션을 폐기한 뒤 현재 기기용 새 세션을 발급 (현재 기기는 로그인 유지, 다른 기기는 로그아웃)
    // accessToken은 그대로 유효하므로 새로 주지 않고, 새 refreshToken만 쿠키로 교체
    @Transactional
    public IssuedSession changePassword(Long userId, ChangePasswordRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 유저 입니다."));
        if(!passwordEncoder.matches(requestDto.currentPassword(), user.getPassword())) {
            throw new ServiceException("400-3", "현재 비밀번호가 일치하지 않습니다.");
        }
        if(passwordEncoder.matches(requestDto.newPassword(), user.getPassword())) {
            throw new ServiceException("400-4", "새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }
        user.changePassword(passwordEncoder.encode(requestDto.newPassword()));
        // 비밀번호 변경 시 모든 기기의 refreshToken 폐기
        refreshTokenRepository.deleteAllByUserId(userId);
        // deleteAllByUserId의 clearAutomatically로 user는 준영속 상태지만, 새 세션은 user의 id만 참조하므로 문제없음
        return issueSession(user);
    }

    @Transactional
    public UserDto promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 유저 입니다."));
        user.promoteToAdmin();
        return new UserDto(user);
    }
}
