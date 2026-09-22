package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedGameRecordProjection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// 게임 기록에서 연관 추천의 출발점으로 사용할 게임을 선택
@Component
public class PersonalizedRecordSourceSelector {

    private static final BigDecimal MIN_SOURCE_RATING = new BigDecimal("3.5");

    // 별점이 없거나 3.5점 이상인 기록 게임의 ID를 중복 없이 반환
    public List<Long> selectSourceGameIds(List<PersonalizedGameRecordProjection> records) {
        return records.stream()
                .filter(record -> record.rating() == null
                        || record.rating().compareTo(MIN_SOURCE_RATING) >= 0)
                .map(PersonalizedGameRecordProjection::gameId)
                .distinct()
                .toList();
    }
    
}
