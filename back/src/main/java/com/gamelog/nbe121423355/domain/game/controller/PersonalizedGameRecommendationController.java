package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.dto.PersonalizedGameRecommendationResponse;
import com.gamelog.nbe121423355.domain.game.service.PersonalizedGameRecommendationService;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 로그인 사용자의 게임 기록 또는 온보딩 선택을 기준으로 맞춤 게임을 추천
@RestController
@RequestMapping("/api/v1/games/recommendations")
@RequiredArgsConstructor
public class PersonalizedGameRecommendationController {

    private final PersonalizedGameRecommendationService recommendationService;

    // 인증 토큰의 사용자 ID를 기준으로 맞춤 추천 게임을 최대 5개 반환
    @GetMapping("/personalized")
    public RsData<List<PersonalizedGameRecommendationResponse>> getPersonalizedRecommendations(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        List<PersonalizedGameRecommendationResponse> response =
                recommendationService.recommend(securityUser.getId());

        return new RsData<>(
                "200-1",
                "사용자 맞춤 추천 게임을 조회했습니다.",
                response
        );
    }
    
}

