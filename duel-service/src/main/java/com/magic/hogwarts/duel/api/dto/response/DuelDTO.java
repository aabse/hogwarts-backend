package com.magic.hogwarts.duel.api.dto.response;

import com.magic.hogwarts.duel.domain.model.DuelStatus;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Duel representation returned by the API.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class DuelDTO {
  /** Duel identifier. */
  private Long id;

  /** Attacker wizard id. */
  private Long attackerId;

  /** Defender wizard id. */
  private Long defenderId;

  /** Current duel status. */
  private DuelStatus status;

  /** Winner wizard id (available when finished). */
  private Long winnerId;

  /** Loser wizard id (available when finished). */
  private Long loserId;

  /** Human-readable message. */
  private String message;

  /** Debug seed used to reproduce the outcome. */
  private Long debugSeed;

  /** Creation timestamp. */
  private OffsetDateTime createdAt;

  /** Finish timestamp (nullable until finished). */
  private OffsetDateTime finishedAt;
}
