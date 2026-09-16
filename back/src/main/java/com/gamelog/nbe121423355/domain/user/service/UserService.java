package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.dto.*;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // 로그인 결과를 Controller에 전달하기 위한 내부 운반용 record
    public record LoginResult(UserDto user, String accessToken, String refreshToken) {
    }

    // 회원가입 메소드
    public UserDto signUp(SignupRequestDto signUpDto) {
        if (userRepository.existsByEmail(signUpDto.email())) {
            throw new ServiceException("409-1", "이미 존재하는 이메일입니다.");
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
        if (userOptional.isEmpty()) {
            throw new ServiceException("401-1", "이메일 또는 비밀번호가 일치하지 않습니다.");
        }
        User user = userOptional.get();
        if (!passwordEncoder.matches(loginRequestDto.password(), user.getPassword())) {
            throw new ServiceException("401-1", "이메일 또는 비밀번호가 일치하지 않습니다.");
        }
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getNickname(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        return new LoginResult(new UserDto(user), accessToken, refreshToken);
    }

    // 토큰 갱신 메소드
    public TokenResponseDto refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new ServiceException("401-2", "유효하지 않은 토큰 입니다.");
        }
        Long userId = jwtProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("401-2", "유효하지 않은 토큰입니다."));

        String newAccessToken = jwtProvider.generateAccessToken(userId, user.getEmail(), user.getNickname(), user.getRole());
        return new TokenResponseDto(newAccessToken);
    }

    // 내정보 가져오기 - 온보딩 확인 여부
    public UserDto getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 유저 입니다."));
        return new UserDto(user);
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
    public UserDto skipOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 유저 입니다."));
        user.completeOnboarding();
        return new UserDto(user);
    }
}