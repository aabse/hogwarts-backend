package com.magic.hogwarts.wizard.api.dto.response;

import lombok.Data;

/**
 * Result of an attack/duel operation.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class AttackResultDTO {
  /** Attacker wizard id. */
  private Long attackerId;

  /** Defender wizard id. */
  private Long defenderId;

  /** Winner wizard id. */
  private Long winnerId;

  /** Loser wizard id. */
  private Long loserId;

  /** Winner level before the duel. */
  private Integer winnerLevelBefore;

  /** Winner level after the duel. */
  private Integer winnerLevelAfter;

  /** Loser level before the duel. */
  private Integer loserLevelBefore;

  /** Loser level after the duel. */
  private Integer loserLevelAfter;

  /** Human-readable message describing the result. */
  private String message;
}
