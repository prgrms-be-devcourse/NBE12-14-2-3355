package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.user.dto.*;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import com.gamelog.nbe121423355.global.security.jwt.JwtProvider;
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
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        Optional<User> userOptional = userRepository.findByEmail(loginRequestDto.email());
        if(userOptional.isEmpty()) {
            throw new ServiceException("401-1", "이메일 또는 비밀번호가 일치하지 않습니다.");
        }
        User user = userOptional.get();
        if(!passwordEncoder.matches(loginRequestDto.password(), user.getPassword())){
            throw new ServiceException("401-1", "이메일 또는 비밀번호가 일치하지 않습니다.");
        }
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        return new LoginResponseDto(new UserDto(user), accessToken);
    }

    // 토큰 갱신 메소드
    public TokenResponseDto refresh(String refreshToken) {
        if(!jwtProvider.validateToken(refreshToken)) {
            throw new ServiceException("401-2", "유효하지 않은 토큰 입니다.");
        }
        Long userId = jwtProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("401-2", "유효하지 않은 토큰입니다."));

        String newAccessToken = jwtProvider.generateAccessToken(userId, user.getEmail(), user.getRole());
        return new TokenResponseDto(newAccessToken);
    }
}
