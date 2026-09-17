package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.dto.GameFilterResponse;
import com.gamelog.nbe121423355.domain.game.repository.GenreRepository;
import com.gamelog.nbe121423355.domain.game.repository.PlatformRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameFilterService {
    private final GenreRepository genreRepository;
    private final PlatformRepository platformRepository;

    @Transactional(readOnly = true)
    public GameFilterResponse getFilters() {
        List<GameFilterResponse.Option> genres = genreRepository.findAll(Sort.by("name"))
                .stream()
                .map(genre -> new GameFilterResponse.Option(genre.getId(), genre.getName()))
                .toList();

        List<GameFilterResponse.Option> platforms = platformRepository.findAll(Sort.by("name"))
                .stream()
                .map(platform -> new GameFilterResponse.Option(platform.getId(), platform.getName()))
                .toList();

        return new GameFilterResponse(genres, platforms);
    }
}
