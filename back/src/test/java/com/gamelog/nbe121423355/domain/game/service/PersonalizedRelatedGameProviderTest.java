package com.gamelog.nbe121423355.domain.game.service;

import com.gamelog.nbe121423355.domain.game.repository.PersonalizedRelatedGameQueryRepository;
import com.gamelog.nbe121423355.domain.game.repository.projection.PersonalizedRelatedGameProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalizedRelatedGameProviderTest {

    @Mock
    private PersonalizedRelatedGameQueryRepository repository;
    private PersonalizedRelatedGameProvider provider;

    @BeforeEach
    void setUp() {
        provider = new PersonalizedRelatedGameProvider(repository);
        ReflectionTestUtils.setField(provider, "precomputedEnabled", true);
    }

    @Test
    void emptySourcesDoNotQuery() {
        assertThat(provider.findTopFiveBySourceGameIds(List.of(), List.of())).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void disabledUsesOnlyExistingQuery() {
        ReflectionTestUtils.setField(provider, "precomputedEnabled", false);
        when(repository.findTopFiveBySourceGameIds(List.of(1L), List.of(9L))).thenReturn(List.of());
        provider.findTopFiveBySourceGameIds(List.of(1L), List.of(9L));
        verify(repository).findTopFiveBySourceGameIds(List.of(1L), List.of(9L));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void sufficientStoredCandidatesAvoidRealtimeQuery() {
        var stored = five(1L);
        when(repository.findStoredTopFiveBySourceGameIds(List.of(1L), List.of())).thenReturn(stored);

        assertThat(provider.findTopFiveBySourceGameIds(List.of(1L, 1L), List.of())).isEqualTo(stored);
        verify(repository).findStoredTopFiveBySourceGameIds(List.of(1L), List.of());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void replacesOnlyInsufficientSourcesInOneQueryWithoutDoubleCounting() {
        var complete = five(1L);
        var stale = row(2L, 99L, 1);
        when(repository.findStoredTopFiveBySourceGameIds(List.of(1L, 2L, 3L), List.of(8L)))
                .thenReturn(Stream.concat(complete.stream(), Stream.of(stale)).toList());
        var replacement = List.of(row(2L, 20L, 1));
        when(repository.findTopFiveBySourceGameIds(List.of(2L, 3L), List.of(8L))).thenReturn(replacement);

        var result = provider.findTopFiveBySourceGameIds(List.of(3L, 2L, 1L), List.of(8L));

        assertThat(result).containsExactlyElementsOf(Stream.concat(complete.stream(), replacement.stream()).toList());
        assertThat(result).doesNotContain(stale);
        verify(repository).findStoredTopFiveBySourceGameIds(List.of(1L, 2L, 3L), List.of(8L));
        verify(repository).findTopFiveBySourceGameIds(List.of(2L, 3L), List.of(8L));
        verifyNoMoreInteractions(repository);
    }

    private List<PersonalizedRelatedGameProjection> five(long source) {
        return IntStream.rangeClosed(1, 5).mapToObj(i -> row(source, source * 10 + i, i)).toList();
    }

    private PersonalizedRelatedGameProjection row(long source, long candidate, int rank) {
        return new PersonalizedRelatedGameProjection(source, candidate, "후보", null,
                new BigDecimal("80"), new BigDecimal("999"), rank);
    }
}
