package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.user.dto.PreferredGameResponseDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGame;
import com.gamelog.nbe121423355.domain.user.repository.UserPreferenceGameRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPreferenceGameService {

    private final UserPreferenceGameRepository userPreferenceGameRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    // 온보딩 처음 선택 + 나중에 다시 바꾸기 구분하지 않는 로직
    @Transactional
    public List<PreferredGameResponseDto> updatePreferredGames(Long userId, List<Long> genreIds) {
        if(genreIds.size() > 3) {
            throw new ServiceException("400-2", "선호 게임은 최대 3개까지 선택 가능합니다."); // 온보딩 장르 최대 3개 선택
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 유저 입니다."));
        List<Game> games = gameRepository.findAllById(genreIds);
        if(games.size() != genreIds.size()) {
            throw new ServiceException("400-1", "존재하지 않는 게임이 포함되어 있습니다.");
        }
        userPreferenceGameRepository.deleteByUser_Id(userId);

        List<UserPreferenceGame> newPreferences = games.stream()
                .map(game -> new UserPreferenceGame(user, game))
                .toList();
        userPreferenceGameRepository.saveAll(newPreferences);

        return newPreferences.stream()
                .map(PreferredGameResponseDto::new)
                .toList();
    }

    // 온보딩 게임정보 가져오기
    public List<PreferredGameResponseDto> getPreferredGames(Long userId) {
        return userPreferenceGameRepository.findByUser_Id(userId).stream()
                .map(PreferredGameResponseDto::new)
                .toList();
    }
}
