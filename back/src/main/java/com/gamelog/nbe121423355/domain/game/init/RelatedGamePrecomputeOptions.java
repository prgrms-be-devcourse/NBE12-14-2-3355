package com.gamelog.nbe121423355.domain.game.init;

import com.gamelog.nbe121423355.domain.game.service.model.RelatedGamePrecomputeMode;
import org.springframework.boot.ApplicationArguments;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public record RelatedGamePrecomputeOptions(boolean all, List<Long> gameIds, RelatedGamePrecomputeMode mode) {

    public static RelatedGamePrecomputeOptions parse(ApplicationArguments arguments) {
        if (!arguments.getNonOptionArgs().isEmpty()
                || !Set.of("all", "game-ids", "mode").containsAll(arguments.getOptionNames())) {
            throw new IllegalArgumentException("지원 옵션: --all 또는 --game-ids=1,2 및 --mode=INITIAL_LOAD|REFRESH");
        }
        boolean all = arguments.containsOption("all");
        boolean specified = arguments.containsOption("game-ids");
        if (all == specified) {
            throw new IllegalArgumentException("--all과 --game-ids 중 하나만 지정하세요.");
        }
        if (all && !arguments.getOptionValues("all").isEmpty()) {
            throw new IllegalArgumentException("--all은 값 없이 지정하세요.");
        }
        RelatedGamePrecomputeMode mode;
        try {
            mode = RelatedGamePrecomputeMode.valueOf(singleValue(arguments, "mode"));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("--mode=INITIAL_LOAD 또는 --mode=REFRESH를 지정하세요.", failure);
        }
        List<Long> ids = List.of();
        if (specified) {
            try {
                ids = Arrays.stream(singleValue(arguments, "game-ids").split(",", -1))
                        .map(String::strip).map(Long::valueOf).distinct().sorted().toList();
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException("게임 ID는 쉼표로 구분한 양수여야 합니다.", failure);
            }
            if (ids.stream().anyMatch(id -> id < 1)) {
                throw new IllegalArgumentException("게임 ID는 양수여야 합니다.");
            }
        }
        return new RelatedGamePrecomputeOptions(all, ids, mode);
    }

    private static String singleValue(ApplicationArguments arguments, String name) {
        List<String> values = arguments.getOptionValues(name);
        if (values == null || values.size() != 1 || values.getFirst().isBlank()) {
            throw new IllegalArgumentException("--" + name + " 값을 한 번 지정하세요.");
        }
        return values.getFirst();
    }
}
