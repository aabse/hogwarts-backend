package com.magic.hogwarts.duel.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Duel JPA entity (write model) owned by the duel service.
 *
 * <p>Note: controllers should never expose this entity directly; they should return DTOs.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Entity
@Data
public class Duel {
  /** Database identifier. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Attacker wizard id. */
  private Long attackerId;

  /** Defender wizard id. */
  private Long defenderId;

  /** Duel status. */
  @Enumerated(EnumType.STRING)
  private DuelStatus status;

  /** Winner wizard id (nullable until finished). */
  private Long winnerId;

  /** Loser wizard id (nullable until finished). */
  private Long loserId;

  /** Human-readable message describing the result. */
  private String message;

  /** Seed used to decide the result in a reproducible way. */
  private Long seed;

  /** Creation timestamp. */
  private OffsetDateTime createdAt;

  /** Finish timestamp (nullable until finished). */
  private OffsetDateTime finishedAt;

  /** Optimistic locking version column. */
  @Version private Long version;
}
