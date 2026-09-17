package com.gamelog.nbe121423355.domain.game.controller;

import com.gamelog.nbe121423355.domain.game.repository.GenreRepository;
import com.gamelog.nbe121423355.domain.game.repository.PlatformRepository;
import com.gamelog.nbe121423355.global.dto.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameFilterController {
    private final GenreRepository genreRepository;
    private final PlatformRepository platformRepository;

    @GetMapping("/filters")
    public RsData<FilterOptions> filters() {
        return new RsData<>("200-1", "게임 필터 목록을 조회했습니다.", new FilterOptions(
                genreRepository.findAll(Sort.by("name")).stream()
                        .map(genre -> new Option(genre.getId(), genre.getName())).toList(),
                platformRepository.findAll(Sort.by("name")).stream()
                        .map(platform -> new Option(platform.getId(), platform.getName())).toList()
        ));
    }

    public record Option(Long id, String name) {}
    public record FilterOptions(List<Option> genres, List<Option> platforms) {}
}
