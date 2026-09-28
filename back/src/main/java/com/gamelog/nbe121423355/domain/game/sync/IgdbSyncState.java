package com.gamelog.nbe121423355.domain.game.sync;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "igdb_sync_state")
@Getter
public class IgdbSyncState {
    @Id
    Long id = 1L;
    String status = "IDLE";
    String mode;
    String runId;
    Long requestedBy;
    Long watermark;
    Long sinceEpoch;
    long cutoff;
    long cursorId;
    long processed;
    long inserted;
    long updated;
    long deleted;
    long skipped;
    long leaseUntil;
    Long finishedAt;
    @Column(length = 500)
    String error;
}
