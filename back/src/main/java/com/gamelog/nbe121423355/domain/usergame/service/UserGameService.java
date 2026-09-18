package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.Platform;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.repository.PlatformRepository;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.*;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserGameService {

    private final UserGameRepository userGameRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final PlatformRepository platformRepository;
    private final ReviewRepository reviewRepository;
    private static final BigDecimal LONG_PLAY_HOURS =
            BigDecimal.valueOf(30);

    @Transactional(readOnly = true)
    public Page<UserGameListResponse> getUserGameList(Long userId, Pageable pageable){

        return userGameRepository.findAllByUser_IdAndInLibraryTrue(userId, pageable)
                .map(UserGameListResponse::new);
    }

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

    private UserGame createUserGame(Long userId, Long gameId) {

        User user = findUser(userId);
        Game game = findGame(gameId);

        return new UserGame(
                user,
                game,
                null,
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private UserGame getOrCreateUserGame(Long userId, Long gameId) {

        return userGameRepository.findByUser_IdAndGame_Id(userId, gameId)
                .orElseGet(() -> userGameRepository.save(
                        createUserGame(userId, gameId)
                ));
    }

    private UserGame findOrCreateUserGameForBoolean(
            Long userId,
            Long gameId,
            boolean status
    ) {
        Optional<UserGame> userGame =
                userGameRepository.findByUser_IdAndGame_Id(userId, gameId);

        if (userGame.isPresent()) {
            return userGame.get();
        }

        if (!status) {
            throw new ServiceException(
                    "404-3",
                    "라이브러리에 등록되지 않은 게임입니다."
            );
        }

        return userGameRepository.save(
                createUserGame(userId, gameId)
        );
    }

    @Transactional
    public PlayStatus changePlayed(Long userId, Long gameId, PlayStatus playStatus) {

        if (playStatus == null) {
            UserGame userGame = findUserGame(userId, gameId);
            userGame.clearPlayStatus();

            return null;
        }

        UserGame userGame = getOrCreateUserGame(userId, gameId);
        userGame.changePlayStatus(playStatus);

        return playStatus;
    }

    @Transactional
    public boolean changePlaying(Long userId, Long gameId, boolean playing) {

        UserGame userGame = findOrCreateUserGameForBoolean(
                userId, gameId, playing
        );

        userGame.changePlaying(playing);

        return playing;
    }

    @Transactional
    public boolean changeWishlist(Long userId, Long gameId, boolean wishlist) {

        UserGame userGame = findOrCreateUserGameForBoolean(
                userId, gameId, wishlist
        );

        userGame.changeWishlist(wishlist);

        return wishlist;
    }

    @Transactional
    public boolean changeBacklog(Long userId, Long gameId, boolean backlog) {

        UserGame userGame = findOrCreateUserGameForBoolean(
                userId, gameId, backlog
        );

        userGame.changeBacklog(backlog);

        return backlog;
    }

    @Transactional
    public boolean changeLiked(Long userId, Long gameId, boolean liked) {

        UserGame userGame = findOrCreateUserGameForBoolean(
                userId, gameId, liked
        );

        userGame.changeLiked(liked);

        return liked;
    }

    @Transactional(readOnly = true)
    public UserGameTasteResponse profileTab(Long userId){
        List<UserGame> userGames =
                userGameRepository.findPlayedGames(userId);

        //플레이 요약 카운터
        long playedGameCount = userGames.size();
        BigDecimal averageRating = reviewRepository.findAverageRating(userId);
        BigDecimal totalPlayTime = userGames.stream()
                .map(UserGame::getPlayTimeHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 산점도
        List<UserGameScatterResponse> scatterData =
                userGameRepository.findPlayedGameScatterData(userId);

        //내 게임 취향 한번에 보기
        //30시간 이상 플레이 비율
        List<UserGame> gamesWithPlayTime = userGames.stream()
                .filter(ug -> ug.getPlayTimeHours() != null)
                .toList();

        long longPlayCount = gamesWithPlayTime.stream()
                .filter(ug ->
                        ug.getPlayTimeHours()
                                .compareTo(LONG_PLAY_HOURS) >= 0
                )
                .count();

        BigDecimal longPlayRatio = BigDecimal.ZERO;

        if (!gamesWithPlayTime.isEmpty()) {
            longPlayRatio = BigDecimal.valueOf(longPlayCount)
                    .divide(
                            BigDecimal.valueOf(gamesWithPlayTime.size()),
                            4,
                            RoundingMode.HALF_UP
                    );
        }

        String timePlayMessage =
                createLongPlayMessage(longPlayRatio);

        //높은 평점 비율
        long highRatedCount =
                reviewRepository.countHighRatedReviews(userId);
        long reviewCount =
                reviewRepository.countRatedReviews(userId);

        BigDecimal highRatingRatio = BigDecimal.ZERO;

        if (reviewCount > 0) {
            highRatingRatio = BigDecimal.valueOf(highRatedCount)
                    .divide(
                            BigDecimal.valueOf(reviewCount),
                            4,
                            RoundingMode.HALF_UP
                    );
        }

        String highRatingMessage =
                createHighRatingMessage(highRatingRatio);

        //완료 게임 비율
        long completedCount = userGames.stream()
                .filter(ug -> ug.getCompletedAt() != null)
                .count();

        BigDecimal completionRatio = BigDecimal.ZERO;

        if (!userGames.isEmpty()) {
            completionRatio = BigDecimal.valueOf(completedCount)
                    .divide(
                            BigDecimal.valueOf(userGames.size()),
                            4,
                            RoundingMode.HALF_UP
                    );
        }

        String completionMessage =
                createCompletionMessage(completionRatio);

//        ProfileStatsResponse statsResponse = new ProfileStatsResponse(playedGameCount,averageRating,totalPlayTime);
//        return new UserProfileResponse(statsResponse,scatterData);
        return new UserGameTasteResponse(
                longPlayRatio,
                timePlayMessage,
                highRatingRatio,
                highRatingMessage,
                completionRatio,
                completionMessage
        );
    }

    private String createLongPlayMessage(BigDecimal ratio) {

        if (ratio.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
            return "장시간 플레이하는 게임이 많아요.";
        }

        if (ratio.compareTo(BigDecimal.valueOf(0.4)) >= 0) {
            return "장시간 플레이와 짧은 게임을 골고루 즐겨요.";
        }

        return "짧게 플레이하는 게임이 많아요.";
    }

    private String createHighRatingMessage(BigDecimal ratio) {

        if (ratio.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
            return "높은 평점을 주는 게임이 많아요.";
        }

        if (ratio.compareTo(BigDecimal.valueOf(0.4)) >= 0) {
            return "게임을 비교적 후하게 평가하는 편이에요.";
        }

        return "평점을 신중하게 주는 편이에요.";
    }


    private String createCompletionMessage(BigDecimal ratio) {

        if (ratio.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
            return "게임을 끝까지 플레이하는 편이에요.";
        }

        if (ratio.compareTo(BigDecimal.valueOf(0.4)) >= 0) {
            return "플레이한 게임 중 절반 정도를 완료했어요.";
        }

        return "다양한 게임을 경험하는 편이에요.";
    }
}
