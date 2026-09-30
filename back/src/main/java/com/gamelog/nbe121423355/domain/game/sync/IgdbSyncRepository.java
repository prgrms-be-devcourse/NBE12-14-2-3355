package com.gamelog.nbe121423355.domain.game.sync;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface IgdbSyncRepository extends JpaRepository<IgdbSyncState, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from IgdbSyncState s where s.id = 1")
    Optional<IgdbSyncState> lockState();
}
