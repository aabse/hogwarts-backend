package com.magic.hogwarts.wizard.application;

import com.magic.hogwarts.wizard.api.dto.request.WizardPatchRequestDTO;
import com.magic.hogwarts.wizard.api.dto.request.WizardRequestDTO;
import com.magic.hogwarts.wizard.api.dto.response.AttackResultDTO;
import com.magic.hogwarts.wizard.api.dto.response.WizardDTO;
import com.magic.hogwarts.wizard.domain.model.Wizard;
import com.magic.hogwarts.wizard.infrastructure.persistence.WizardRepository;
import com.magic.hogwarts.wizard.infrastructure.persistence.WizardSpecifications;
import jakarta.transaction.Transactional;
import java.util.NoSuchElementException;
import java.util.Random;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Application service implementing wizard use-cases.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Create and validate wizards
 *   <li>List and filter wizards (with paging)
 *   <li>Patch and delete wizards
 *   <li>Run a simple duel/attack operation (to be moved to duel-service in later phases)
 * </ul>
 *
 * @author Angel Absé
 * @since 1.0
 */
@Service
public class WizardService {

  /** Minimum allowed wizard level. */
  private static final int MIN_LEVEL = 1;

  /** Wizard persistence gateway. */
  private final WizardRepository repository;

  /** Allowed houses for assignment when none provided. */
  private final String[] houses = {"Gryffindor", "Hufflepuff", "Ravenclaw", "Slytherin"};

  /** RNG used for tie-breaking and house assignment. */
  private final Random random = new Random();

  /**
   * Creates the service.
   *
   * @param repository wizard repository
   */
  public WizardService(WizardRepository repository) {
    this.repository = repository;
  }

  /**
   * Creates a wizard.
   *
   * <p>Rules:
   *
   * <ul>
   *   <li>If house is blank, it is randomly assigned.
   *   <li>(house, name) must be unique (case-insensitive).
   * </ul>
   *
   * @param request creation request payload
   * @return created wizard representation
   * @throws IllegalArgumentException if uniqueness rule is violated
   */
  public WizardDTO createWizard(WizardRequestDTO request) {
    Wizard wizard = new Wizard();
    wizard.setHouse(request.getHouse());
    wizard.setName(request.getName());

    // Business logic: the Sorting Hat decides
    if (wizard.getHouse() == null || wizard.getHouse().isBlank()) {
      int randomIndex = random.nextInt(houses.length);
      wizard.setHouse(houses[randomIndex]);
    }

    if (repository.existsByHouseIgnoreCaseAndNameIgnoreCase(wizard.getHouse(), wizard.getName())) {
      throw new IllegalArgumentException("A wizard with that name already exists in that house");
    }

    Wizard saved = repository.save(wizard);
    return toDTO(saved);
  }

  /**
   * Lists wizards using optional filters and pagination.
   *
   * @param house optional house filter (exact match, case-insensitive)
   * @param name optional name filter (contains, case-insensitive)
   * @param pageable paging/sorting information
   * @return a page of matching wizards
   */
  public Page<WizardDTO> list(String house, String name, Pageable pageable) {
    Specification<Wizard> spec = null;

    if (house != null && !house.isBlank()) {
      spec = WizardSpecifications.houseEqualsIgnoreCase(house);
    }
    if (name != null && !name.isBlank()) {
      Specification<Wizard> nameSpec = WizardSpecifications.nameContainsIgnoreCase(name);
      spec = (spec == null) ? nameSpec : spec.and(nameSpec);
    }

    if (spec == null) {
      return repository.findAll(pageable).map(this::toDTO);
    }

    return repository.findAll(spec, pageable).map(this::toDTO);
  }

  /**
   * Retrieves a wizard by id.
   *
   * @param id wizard id
   * @return wizard representation
   * @throws NoSuchElementException if the wizard does not exist
   */
  public WizardDTO getById(Long id) {
    Wizard wizard =
        repository.findById(id).orElseThrow(() -> new NoSuchElementException("Wizard not found"));
    return toDTO(wizard);
  }

  /**
   * Applies a partial update to a wizard.
   *
   * @param id wizard id
   * @param request patch request (non-null fields are applied)
   * @return updated wizard representation
   * @throws NoSuchElementException if the wizard does not exist
   * @throws IllegalArgumentException if uniqueness rule is violated
   */
  @Transactional
  public WizardDTO patch(Long id, WizardPatchRequestDTO request) {
    Wizard wizard =
        repository.findById(id).orElseThrow(() -> new NoSuchElementException("Wizard not found"));

    boolean changed = false;
    if (request.getName() != null) {
      wizard.setName(request.getName());
      changed = true;
    }
    if (request.getHouse() != null) {
      wizard.setHouse(request.getHouse());
      changed = true;
    }

    if (!changed) {
      return toDTO(wizard);
    }

    if (wizard.getHouse() != null
        && wizard.getName() != null
        && repository.existsByHouseIgnoreCaseAndNameIgnoreCase(
            wizard.getHouse(), wizard.getName())) {
      throw new IllegalArgumentException("A wizard with that name already exists in that house");
    }

    return toDTO(repository.save(wizard));
  }

  /**
   * Deletes a wizard by id.
   *
   * @param id wizard id
   * @throws NoSuchElementException if the wizard does not exist
   */
  public void delete(Long id) {
    if (!repository.existsById(id)) {
      throw new NoSuchElementException("Wizard not found");
    }
    repository.deleteById(id);
  }

  /**
   * Attack/duel where the server decides winner/loser. Returns the result including levels
   * before/after.
   *
   * @param attackerId attacker wizard id
   * @param defenderId defender wizard id
   * @return duel result including winner/loser and level changes
   * @throws IllegalArgumentException if ids are missing/invalid
   * @throws NoSuchElementException if either wizard does not exist
   */
  @Retryable(
      retryFor = {ObjectOptimisticLockingFailureException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 500))
  @Transactional
  public AttackResultDTO attack(Long attackerId, Long defenderId) {
    if (attackerId == null || defenderId == null) {
      throw new IllegalArgumentException("attackerId and defenderId are required");
    }
    if (attackerId.equals(defenderId)) {
      throw new IllegalArgumentException("Attacker and defender cannot be the same wizard");
    }

    Wizard attacker =
        repository
            .findById(attackerId)
            .orElseThrow(() -> new NoSuchElementException("Wizard not found"));
    Wizard defender =
        repository
            .findById(defenderId)
            .orElseThrow(() -> new NoSuchElementException("Wizard not found"));

    Wizard winner;
    Wizard loser;
    boolean attackerWon = decideAttackerWins(attacker, defender);
    if (attackerWon) {
      winner = attacker;
      loser = defender;
    } else {
      winner = defender;
      loser = attacker;
    }

    int winnerBefore = levelOrDefault(winner);
    int loserBefore = levelOrDefault(loser);

    applyDuelOutcome(winner, loser);

    AttackResultDTO result = new AttackResultDTO();
    result.setAttackerId(attackerId);
    result.setDefenderId(defenderId);
    result.setWinnerId(winner.getId());
    result.setLoserId(loser.getId());
    result.setWinnerLevelBefore(winnerBefore);
    result.setWinnerLevelAfter(levelOrDefault(winner));
    result.setLoserLevelBefore(loserBefore);
    result.setLoserLevelAfter(levelOrDefault(loser));
    result.setMessage("Duel completed");
    return result;
  }

  /**
   * Applies the duel outcome by incrementing the winner level and decrementing the loser level.
   *
   * @param winner wizard that won the duel
   * @param loser wizard that lost the duel
   */
  private void applyDuelOutcome(Wizard winner, Wizard loser) {
    winner.setLevel(levelOrDefault(winner) + 1);

    int loserLevel = levelOrDefault(loser) - 1;
    loser.setLevel(Math.max(MIN_LEVEL, loserLevel));

    repository.save(winner);
    repository.save(loser);
  }

  /**
   * Determines if the attacker wins based on levels (higher level wins) with random tie-breaker.
   *
   * @param attacker attacker wizard
   * @param defender defender wizard
   * @return {@code true} if attacker wins
   */
  private boolean decideAttackerWins(Wizard attacker, Wizard defender) {
    int attackerLevel = levelOrDefault(attacker);
    int defenderLevel = levelOrDefault(defender);

    if (attackerLevel > defenderLevel) return true;
    if (attackerLevel < defenderLevel) return false;

    // Tie-breaker: random
    return random.nextBoolean();
  }

  /**
   * Returns the wizard level or a default value when missing.
   *
   * @param wizard wizard entity
   * @return wizard level (never less than {@link #MIN_LEVEL})
   */
  private int levelOrDefault(Wizard wizard) {
    if (wizard.getLevel() == null) return MIN_LEVEL;
    return wizard.getLevel();
  }

  /**
   * Maps the entity to the public API DTO.
   *
   * @param w wizard entity
   * @return API response DTO
   */
  private WizardDTO toDTO(Wizard w) {
    WizardDTO dto = new WizardDTO();
    dto.setId(w.getId());
    dto.setName(w.getName());
    dto.setHouse(w.getHouse());
    dto.setLevel(w.getLevel());

    // Boundary: sin wands-service, no resolvemos descripción aquí
    dto.setWandDescription(w.getWandId() != null ? "Wand assigned" : "No wand assigned");
    return dto;
  }
}
