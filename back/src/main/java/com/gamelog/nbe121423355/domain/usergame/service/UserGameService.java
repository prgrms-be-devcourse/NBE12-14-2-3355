package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.Platform;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.repository.PlatformRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameReqBody;
import com.gamelog.nbe121423355.domain.usergame.dto.UserGameSaveResult;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserGameService {

    private final UserGameRepository userGameRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final PlatformRepository platformRepository;

    //생성 또는 수정
    @Transactional
    public UserGameSaveResult addOrUpdateGameToLibrary(
        Long userId,
        Long gameId,
        UserGameReqBody reqBody
    ){
        Platform platform = findPlatform(reqBody.platformId());

        return userGameRepository.findByUser_IdAndGame_Id(userId, gameId)
                .map(userGame -> {
                    applyPlayRecord(userGame, reqBody, platform);

                    return new UserGameSaveResult(userGame, false);
                })
                .orElseGet(() -> {
                    UserGame userGame = createUserGame(userId, gameId, reqBody, platform);

                    return new UserGameSaveResult(userGame,true);
                });
    }

    @Transactional
    public UserGame updatePlayRecord(
            Long userId,
            Long gameId,
            UserGameReqBody reqBody
    ) {
        UserGame userGame = findUserGame(userId, gameId);
        Platform platform = findPlatform(reqBody.platformId());

        applyPlayRecord(userGame, reqBody, platform);

        return userGame;
    }

    private UserGame createUserGame(
            Long userId,
            Long gameId,
            UserGameReqBody reqBody,
            Platform platform
    ) {
        User user = findUser(userId);
        Game game = findGame(gameId);

        UserGame userGame = new UserGame(
                user,
                game,
                reqBody.playStatus(),
                reqBody.isPlaying(),
                reqBody.isBacklog(),
                reqBody.isWishlist(),
                reqBody.isLiked(),
                platform,
                reqBody.playTimeHours(),
                reqBody.finishTimeHours(),
                reqBody.masterTimeHours(),
                reqBody.startedAt(),
                reqBody.completedAt(),
                reqBody.lastPlayedAt()
        );

        return userGameRepository.save(userGame);
    }

    private void applyPlayRecord(
            UserGame userGame,
            UserGameReqBody reqBody,
            Platform platform
    ) {
        userGame.updatePlayRecord(
                reqBody.playStatus(),
                reqBody.isPlaying(),
                reqBody.isBacklog(),
                reqBody.isWishlist(),
                reqBody.isLiked(),
                platform,
                reqBody.playTimeHours(),
                reqBody.finishTimeHours(),
                reqBody.masterTimeHours(),
                reqBody.startedAt(),
                reqBody.completedAt(),
                reqBody.lastPlayedAt()
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ServiceException("404-1", "사용자를 찾을 수 없습니다.")
                );
    }

    private Game findGame(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() ->
                        new ServiceException("404-2", "게임을 찾을 수 없습니다.")
                );
    }

    private UserGame findUserGame(Long userId, Long gameId) {
        return userGameRepository.findByUser_IdAndGame_Id(userId, gameId)
                .orElseThrow(() ->
                        new ServiceException("404-3", "라이브러리에 등록되지 않은 게임입니다.")
                );
    }

    private Platform findPlatform(Long platformId) {
        if (platformId == null) {
            return null;
        }

        return platformRepository.findById(platformId)
                .orElseThrow(() ->
                        new ServiceException("404-4", "플랫폼을 찾을 수 없습니다.")
                );
    }


}
