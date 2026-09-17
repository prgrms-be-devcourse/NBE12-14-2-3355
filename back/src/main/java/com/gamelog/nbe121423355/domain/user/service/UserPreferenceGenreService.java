package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.game.entity.Genre;
import com.gamelog.nbe121423355.domain.game.repository.GenreRepository;
import com.gamelog.nbe121423355.domain.user.dto.PreferredGenreResponseDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.entity.UserPreferenceGenre;
import com.gamelog.nbe121423355.domain.user.repository.UserPreferenceGenreRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPreferenceGenreService {
    private final UserPreferenceGenreRepository userPreferenceGenreRepository;
    private final UserRepository userRepository;
    private final GenreRepository genreRepository;

    // 온보딩 처음 선택 + 나중에 다시 바꾸기 구분하지 않는 로직
    @Transactional
    public List<PreferredGenreResponseDto> updatePreferredGenres(Long userId, List<Long> genreIds) {
        if(genreIds.size() > 3) {
            throw new ServiceException("400-2", "선호 장르는 최대 3개까지 선택 가능합니다."); // 온보딩 장르 최대 3개 선택
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 유저 입니다."));
        List<Genre> genres = genreRepository.findAllById(genreIds);
        if(genres.size() != genreIds.size()) {
            throw new ServiceException("400-1", "존재하지 않는 장르가 포함되어 있습니다.");
        }
        userPreferenceGenreRepository.deleteByUser_Id(userId);

        List<UserPreferenceGenre> newPreferences = genres.stream()
                .map(genre -> new UserPreferenceGenre(user, genre))
                .toList();
        userPreferenceGenreRepository.saveAll(newPreferences);

        return newPreferences.stream()
                .map(PreferredGenreResponseDto::new)
                .toList();
    }

    // 온보딩 장르정보 가져오기
    public List<PreferredGenreResponseDto> getPreferredGenres(Long userId) {
        return userPreferenceGenreRepository.findByUser_Id(userId).stream()
                .map(PreferredGenreResponseDto::new)
                .toList();
    }

}
