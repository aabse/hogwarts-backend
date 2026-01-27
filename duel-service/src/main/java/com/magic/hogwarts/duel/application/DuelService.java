package com.magic.hogwarts.duel.application;

import com.magic.hogwarts.duel.api.dto.request.CreateDuelRequestDTO;
import com.magic.hogwarts.duel.api.dto.response.DuelDTO;
import com.magic.hogwarts.duel.domain.model.Duel;
import com.magic.hogwarts.duel.domain.model.DuelStatus;
import com.magic.hogwarts.duel.infrastructure.persistence.DuelRepository;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Application service implementing duel use-cases.
 *
 * <p>In this phase, a duel is created and resolved immediately. The outcome is made reproducible by
 * persisting a {@code seed} and using deterministic logic based on ids + seed.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Service
public class DuelService {

  /** Duel persistence gateway. */
  private final DuelRepository repository;

  /**
   * Creates the service.
   *
   * @param repository duel repository
   */
  public DuelService(DuelRepository repository) {
    this.repository = repository;
  }

  /**
   * Creates and finishes the duel in the same operation. The result is reproducible: a seed is
   * persisted and the winner/loser is decided based on it.
   *
   * @param request duel creation payload
   * @return created duel representation
   * @throws IllegalArgumentException when {@code attackerId == defenderId}
   */
  @Retryable(
      retryFor = {ObjectOptimisticLockingFailureException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 300))
  @Transactional
  public DuelDTO create(CreateDuelRequestDTO request) {
    Long attackerId = request.getAttackerId();
    Long defenderId = request.getDefenderId();

    if (attackerId.equals(defenderId)) {
      throw new IllegalArgumentException("attackerId and defenderId cannot be the same");
    }

    Duel duel = new Duel();
    duel.setAttackerId(attackerId);
    duel.setDefenderId(defenderId);
    duel.setStatus(DuelStatus.CREATED);
    duel.setCreatedAt(OffsetDateTime.now());

    // Persisted seed for replay/debug.
    long seed = ThreadLocalRandom.current().nextLong();
    duel.setSeed(seed);

    // Finish immediately (Phase 4.1: persist a reproducible result).
    finishDuel(duel);

    Duel saved = repository.save(duel);
    return toDTO(saved);
  }

  /**
   * Retrieves a duel by id.
   *
   * @param id duel id
   * @return duel representation
   * @throws NoSuchElementException if the duel does not exist
   */
  public DuelDTO getById(Long id) {
    Duel duel =
        repository.findById(id).orElseThrow(() -> new NoSuchElementException("Duel not found"));
    return toDTO(duel);
  }

  /**
   * Applies the duel outcome and marks it as finished.
   *
   * @param duel duel entity to update
   */
  private void finishDuel(Duel duel) {
    duel.setStatus(DuelStatus.FINISHED);
    duel.setFinishedAt(OffsetDateTime.now());

    boolean attackerWins =
        decideAttackerWins(duel.getAttackerId(), duel.getDefenderId(), duel.getSeed());
    if (attackerWins) {
      duel.setWinnerId(duel.getAttackerId());
      duel.setLoserId(duel.getDefenderId());
    } else {
      duel.setWinnerId(duel.getDefenderId());
      duel.setLoserId(duel.getAttackerId());
    }

    duel.setMessage("Duel completed");
  }

  /**
   * Deterministic decision based on ids + seed (reproducible). In future phases this can be
   * replaced by level-based rules via integration with wizard-service.
   *
   * @param attackerId attacker wizard id
   * @param defenderId defender wizard id
   * @param seed debug seed
   * @return {@code true} if attacker wins
   */
  private boolean decideAttackerWins(Long attackerId, Long defenderId, Long seed) {
    long a = attackerId == null ? 0L : attackerId;
    long d = defenderId == null ? 0L : defenderId;
    long s = seed == null ? 0L : seed;

    long mixed = mix64(a) ^ mix64(d) ^ mix64(s);
    return (mixed & 1L) == 0L;
  }

  /**
   * 64-bit mixing function (based on SplitMix64 finalizer) to spread bits.
   *
   * @param z input value
   * @return mixed value
   */
  private long mix64(long z) {
    z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
    z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
    return z ^ (z >>> 33);
  }

  /**
   * Maps the entity to the public API DTO.
   *
   * @param d duel entity
   * @return API response DTO
   */
  private DuelDTO toDTO(Duel d) {
    DuelDTO dto = new DuelDTO();
    dto.setId(d.getId());
    dto.setAttackerId(d.getAttackerId());
    dto.setDefenderId(d.getDefenderId());
    dto.setStatus(d.getStatus());
    dto.setWinnerId(d.getWinnerId());
    dto.setLoserId(d.getLoserId());
    dto.setMessage(d.getMessage());
    dto.setDebugSeed(d.getSeed());
    dto.setCreatedAt(d.getCreatedAt());
    dto.setFinishedAt(d.getFinishedAt());
    return dto;
  }
}
