package com.gamelog.nbe121423355.domain.user.service;

import com.gamelog.nbe121423355.domain.game.entity.Genre;
import com.gamelog.nbe121423355.domain.game.repository.GenreRepository;
import com.gamelog.nbe121423355.domain.user.dto.PreferredGenreResponseDto;
import com.gamelog.nbe121423355.domain.user.entity.User;
import com.gamelog.nbe121423355.domain.user.repository.UserPreferenceGenreRepository;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPreferenceGenreService {
    private final UserPreferenceGenreRepository userPreferenceGenreRepository;
    private final UserRepository userRepository;
    private final GenreRepository genreRepository;

    public List<PreferredGenreResponseDto> updatePreferredGenres(Long userId, List<Long> genreIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 유저 입니다."));
        List<Genre> genres = genreRepository.findAllById(genreIds);
        if(genres.size() == )
    }
    public List<PreferredGenreResponseDto> getPreferredGenres(Long userId) {

    }
}
