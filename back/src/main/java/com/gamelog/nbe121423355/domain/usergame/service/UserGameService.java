package com.gamelog.nbe121423355.domain.usergame.service;

import com.gamelog.nbe121423355.domain.game.entity.Game;
import com.gamelog.nbe121423355.domain.game.entity.Platform;
import com.gamelog.nbe121423355.domain.game.repository.GameRepository;
import com.gamelog.nbe121423355.domain.game.repository.PlatformRepository;
import com.gamelog.nbe121423355.domain.review.repository.ReviewRepository;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.entity.UserFavoriteGame;
import com.gamelog.nbe121423355.domain.user.repository.UserFavoriteGameRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserFollowRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.domain.usergame.dto.*;
import com.gamelog.nbe121423355.domain.usergame.entity.PlayStatus;
import com.gamelog.nbe121423355.domain.usergame.entity.UserGame;
import com.gamelog.nbe121423355.domain.usergame.repository.UserGameRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class UserGameService {

    private final UserGameRepository userGameRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final PlatformRepository platformRepository;
    private final ReviewRepository reviewRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserFavoriteGameRepository userFavoriteGameRepository;

    private static final BigDecimal LONG_PLAY_HOURS = BigDecimal.valueOf(30);
    private static final BigDecimal HIGH_RATIO = BigDecimal.valueOf(0.7);
    private static final BigDecimal MEDIUM_RATIO = BigDecimal.valueOf(0.4);
    private static final BigDecimal HIGH_RATING = BigDecimal.valueOf(4.0);
    private static final BigDecimal LOW_RATING = BigDecimal.valueOf(2.5);
    private static final int MIN_DATA_COUNT = 3;


    @Transactional(readOnly = true)
    public Page<UserGameListResponse> getUserGameList(Long userId, Pageable pageable){

        return userGameRepository.findAllByUser_IdAndInLibraryTrue(userId, pageable)
                .map(UserGameListResponse::new);
    }

    @Transactional(readOnly = true)
    public Page<UserGameListResponse> getUserGameList(Long userId, UserGameSearchRequest request) {
        String keyword = request.getKeyword();
        String pattern = keyword == null || keyword.isBlank() ? null
                : "%" + keyword.strip().replace("!", "!!").replace("%", "!%")
                        .replace("_", "!_") + "%";
        boolean platforms = request.getPlatformIds() != null && !request.getPlatformIds().isEmpty();
        boolean genres = request.getGenreIds() != null && !request.getGenreIds().isEmpty();
        return userGameRepository.findByLibraryFilters(userId, request.getStatus().name(), pattern,
                        platforms, platforms ? request.getPlatformIds() : List.of(0L),
                        genres, genres ? request.getGenreIds() : List.of(0L),
                        request.getSort().name(),
                        org.springframework.data.domain.PageRequest.of(request.getPage(), request.getSize()))
                .map(UserGameListResponse::new);
    }

    @Transactional(readOnly = true)
    public Page<UserGameListResponse> getPublicUserGameList(Long userId, UserGameSearchRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new ServiceException("404-1", "존재하지 않는 유저입니다.");
        }
        return getUserGameList(userId, request);
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

    //프로필 탭
    @Transactional(readOnly = true)
    public UserProfileResponse profileTab(Long targetUserId, Long currentUserId){
        if (!userRepository.existsById(targetUserId)) {
            throw new ServiceException("404-1", "존재하지 않는 유저입니다.");
        }
        List<UserGame> playedGames =
                userGameRepository.findPlayedGames(targetUserId);

        List<BigDecimal> ratings =
                reviewRepository.findPlayedGameRatings(targetUserId);

        //인생게임
        List<UserFavoriteGameResponse> favoriteGames =
                getFavoriteGames(targetUserId);

        //플레이 요약 카운터
        long playedGameCount = playedGames.size();
        BigDecimal averageRating = reviewRepository.findAverageRating(targetUserId);
        BigDecimal totalPlayTime = playedGames.stream()
                .map(UserGame::getPlayTimeHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 산점도
        List<UserGameScatterDto> scatterData =
                userGameRepository.findPlayedGameScatterData(targetUserId);

        //내 게임 취향 한번에 보기
        //30시간 이상 플레이 비율
        TasteMetricDto longPlay =
                calculateLongPlay(playedGames);

        //별점 기반
        TasteMetricDto rating =
                calculateRating(ratings);

        //완료율 기반
        TasteMetricDto completion =
                calculateCompletion(playedGames);

        //장르 분포
        List<UserGameGenreDistributionResponse> genreDistribution =
                getGenreDistribution(targetUserId);

        //최근 플레이 게임
        List<UserGameListResponse> RecentPlayedGames = getRecentPlayedGames(targetUserId);

        //최근 리뷰
        List<RecentReviewResponse> recentReviews = getRecentReviews(targetUserId);

        boolean isMe =
                currentUserId != null &&
                        targetUserId.equals(currentUserId);

        boolean isFollowing =
                currentUserId != null &&
                        !isMe &&
                        userFollowRepository.existsByFollowerIdAndFolloweeId(
                                currentUserId,
                                targetUserId
                        );

        ProfileStatsDto statsResponse = new ProfileStatsDto(playedGameCount,averageRating,totalPlayTime);
        UserGameTasteDto tasteResponse = new UserGameTasteDto(
                longPlay,
                rating,
                completion
        );
        return new UserProfileResponse(targetUserId, isMe, isFollowing,favoriteGames, statsResponse, scatterData, tasteResponse, genreDistribution, RecentPlayedGames, recentReviews);
    }

    public List<UserFavoriteGameResponse> getFavoriteGames(Long userId) {
        return userFavoriteGameRepository
                .findAllByUserIdOrderByDisplayOrderAsc(userId)
                .stream()
                .map(UserFavoriteGameResponse::new)
                .toList();
    }

    @Transactional
    public List<UserFavoriteGameResponse> updateFavoriteGames(
            Long userId,
            List<Long> gameIds
    ) {
        // 1. 최대 5개 검증
        if (gameIds == null || gameIds.size() > 5) {
            throw new IllegalArgumentException(
                    "인생게임은 최대 5개까지 등록할 수 있습니다."
            );
        }

        // 2. 중복 게임 검증
        if (gameIds.stream().distinct().count() != gameIds.size()) {
            throw new IllegalArgumentException(
                    "같은 게임을 중복해서 등록할 수 없습니다."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다.")
                );

        // 3. 사용자의 UserGame 조회
        List<UserGame> userGames =
                userGameRepository.findAllByUserIdAndGameIdIn(
                        userId,
                        gameIds
                );

        // 4. 모든 게임이 사용자의 라이브러리에 있는지 검증
        if (userGames.size() != gameIds.size()) {
            throw new IllegalArgumentException(
                    "내 라이브러리에 등록된 게임만 인생게임으로 선택할 수 있습니다."
            );
        }

        // 5. gameId -> Game 매핑
        Map<Long, Game> gameMap = userGames.stream()
                .collect(Collectors.toMap(
                        ug -> ug.getGame().getId(),
                        UserGame::getGame
                ));

        // 6. 기존 인생게임 목록 삭제
        userFavoriteGameRepository.deleteAllByUserId(userId);

        // 7. 요청 순서대로 새 인생게임 생성
        List<UserFavoriteGame> favoriteGames =
                IntStream.range(0, gameIds.size())
                        .mapToObj(index -> {
                            Long gameId = gameIds.get(index);

                            return new UserFavoriteGame(
                                    user,
                                    gameMap.get(gameId),
                                    index + 1
                            );
                        })
                        .toList();

        userFavoriteGameRepository.saveAll(favoriteGames);

        return favoriteGames.stream()
                .map(UserFavoriteGameResponse::new)
                .toList();
    }

    private TasteMetricDto calculateLongPlay(
            List<UserGame> playedGames
    ) {

        List<BigDecimal> playTimes = playedGames.stream()
                .map(UserGame::getPlayTimeHours)
                .filter(Objects::nonNull)
                .toList();

        if (playTimes.size() < MIN_DATA_COUNT) {
            return new TasteMetricDto(
                    null,
                    "아직 플레이 기록이 부족해요.",
                    "플레이 시간 기록이 3개 이상 쌓이면 확인할 수 있어요."
            );
        }

        long longPlayCount = playTimes.stream()
                .filter(playTime ->
                        playTime.compareTo(LONG_PLAY_HOURS) >= 0
                )
                .count();

        BigDecimal longPlayRatio =
                calculateRatio(longPlayCount, playTimes.size());

        // 70% 이상
        if (longPlayRatio.compareTo(HIGH_RATIO) >= 0) {

            return new TasteMetricDto(
                    longPlayRatio,
                    "장시간 플레이하는 게임이 많아요.",
                    createPercentageDescription(
                            longPlayRatio,
                            "30시간 이상 플레이한 게임이"
                    )
            );
        }

        // 40% 이상
        if (longPlayRatio.compareTo(MEDIUM_RATIO) >= 0) {

            return new TasteMetricDto(
                    longPlayRatio,
                    "장시간 플레이와 짧은 게임을 골고루 즐겨요.",
                    createPercentageDescription(
                            longPlayRatio,
                            "30시간 이상 플레이한 게임이"
                    )
            );
        }

        // 40% 미만이면 짧은 게임 비율을 보여준다.
        BigDecimal shortPlayRatio =
                BigDecimal.ONE.subtract(longPlayRatio);

        return new TasteMetricDto(
                shortPlayRatio,
                "짧게 플레이하는 게임이 많아요.",
                createPercentageDescription(
                        shortPlayRatio,
                        "30시간 미만 플레이한 게임이"
                )
        );
    }

    private TasteMetricDto calculateRating(
            List<BigDecimal> ratings
    ) {

        if (ratings.size() < MIN_DATA_COUNT) {
            return new TasteMetricDto(
                    null,
                    "아직 평가 기록이 부족해요.",
                    "게임 평가가 3개 이상 쌓이면 확인할 수 있어요."
            );
        }

        long highRatingCount = ratings.stream()
                .filter(rating ->
                        rating.compareTo(HIGH_RATING) >= 0
                )
                .count();

        long mediumRatingCount = ratings.stream()
                .filter(rating ->
                        rating.compareTo(LOW_RATING) >= 0
                                && rating.compareTo(HIGH_RATING) < 0
                )
                .count();

        long lowRatingCount = ratings.stream()
                .filter(rating ->
                        rating.compareTo(LOW_RATING) < 0
                )
                .count();

        BigDecimal highRatingRatio =
                calculateRatio(highRatingCount, ratings.size());

        BigDecimal mediumRatingRatio =
                calculateRatio(mediumRatingCount, ratings.size());

        BigDecimal lowRatingRatio =
                calculateRatio(lowRatingCount, ratings.size());

        // ① 2.5 ~ 4.0 구간이 가장 크거나 동률인 경우
        if (mediumRatingRatio.compareTo(highRatingRatio) >= 0
                && mediumRatingRatio.compareTo(lowRatingRatio) >= 0) {

            return new TasteMetricDto(
                    mediumRatingRatio,
                    "게임을 비교적 후하게 평가하는 편이에요.",
                    createPercentageDescription(
                            mediumRatingRatio,
                            "2.5점 이상 4.0점 미만으로 평가한 게임이"
                    )
            );
        }


        // ② 4.0 이상 구간이 가장 큰 경우
        if (highRatingRatio.compareTo(lowRatingRatio) >= 0) {

            return new TasteMetricDto(
                    highRatingRatio,
                    "높은 평점을 주는 게임이 많아요.",
                    createPercentageDescription(
                            highRatingRatio,
                            "4.0점 이상 평가한 게임이"
                    )
            );
        }


        // ③ 2.5 미만 구간이 가장 큰 경우
        return new TasteMetricDto(
                lowRatingRatio,
                "평점을 신중하게 주는 편이에요.",
                createPercentageDescription(
                        lowRatingRatio,
                        "2.5점 미만으로 평가한 게임이"
                )
        );
    }

    private TasteMetricDto calculateCompletion(
            List<UserGame> playedGames
    ) {

        if (playedGames.size() < MIN_DATA_COUNT) {
            return new TasteMetricDto(
                    null,
                    "아직 게임 기록이 부족해요.",
                    "플레이한 게임이 3개 이상 쌓이면 확인할 수 있어요."
            );
        }

        long completedCount = playedGames.stream()
                .filter(ug -> PlayStatus.COMPLETED.equals(ug.getPlayStatus()))
                .count();

        BigDecimal completionRatio =
                calculateRatio(
                        completedCount,
                        playedGames.size()
                );

        // 70% 이상
        if (completionRatio.compareTo(HIGH_RATIO) >= 0) {

            return new TasteMetricDto(
                    completionRatio,
                    "게임을 끝까지 플레이하는 편이에요.",
                    createPercentageDescription(
                            completionRatio,
                            "완료한 게임이"
                    )
            );
        }

        // 40% 이상
        if (completionRatio.compareTo(MEDIUM_RATIO) >= 0) {

            return new TasteMetricDto(
                    completionRatio,
                    "플레이한 게임 중 절반 정도를 완료했어요.",
                    createPercentageDescription(
                            completionRatio,
                            "완료한 게임이"
                    )
            );
        }

        return new TasteMetricDto(
                completionRatio,
                "끝까지 완료하기보다 여러 게임을 플레이하는 편이에요.",
                createPercentageDescription(
                        completionRatio,
                        "완료한 게임이"
                )
        );
    }

    private BigDecimal calculateRatio(
            long count,
            int total
    ) {

        return BigDecimal.valueOf(count)
                .divide(
                        BigDecimal.valueOf(total),
                        4,
                        RoundingMode.HALF_UP
                );
    }

    private String createPercentageDescription(
            BigDecimal ratio,
            String subject
    ) {

        BigDecimal percentage = ratio
                .multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros();

        return subject + " "
                + percentage.toPlainString()
                + "%예요.";
    }

    public List<UserGameGenreDistributionResponse> getGenreDistribution(Long userId) {

        List<UserGameGenreDTO> distributions =
                userGameRepository.findGenreDistribution(userId);

        if (distributions.isEmpty()) {
            return List.of();
        }

        long totalGenreGameCount = distributions.stream()
                .mapToLong(UserGameGenreDTO::gameCount)
                .sum();

        return distributions.stream()
                .map(dto -> {
                    BigDecimal ratio = BigDecimal.valueOf(dto.gameCount())
                            .divide(
                                    BigDecimal.valueOf(totalGenreGameCount),
                                    4,
                                    RoundingMode.HALF_UP
                            );

                    return new UserGameGenreDistributionResponse(
                            dto.genreName(),
                            ratio
                    );
                })
                .toList();
    }

    public List<UserGameListResponse> getRecentPlayedGames(Long userId) {

        Pageable pageable = PageRequest.of(0, 5);

        return userGameRepository
                .findRecentPlayedGames(userId, pageable)
                .stream()
                .map(UserGameListResponse::new)
                .toList();
    }

    public List<RecentReviewResponse> getRecentReviews(Long userId) {

        Pageable pageable = PageRequest.of(0, 3);

        return reviewRepository
                .findRecentReviews(userId, pageable)
                .stream()
                .map(RecentReviewResponse::new)
                .toList();
    }
}
