package com.magic.hogwarts.inventory.infrastructure.persistence;

import com.magic.hogwarts.inventory.domain.model.Wand;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory for reusable {@link Specification} objects for {@link Wand} queries.
 *
 * @author Angel Absé
 * @since 1.0
 */
public final class WandSpecifications {
  /** Utility class. */
  private WandSpecifications() {}

  /**
   * Filters by exact wood (case-insensitive).
   *
   * @param wood wood type
   * @return specification to filter by wood
   */
  public static Specification<Wand> woodEqualsIgnoreCase(String wood) {
    return (root, query, cb) -> cb.equal(cb.lower(root.get("wood")), wood.toLowerCase());
  }

  /**
   * Filters by exact core (case-insensitive).
   *
   * @param core wand core
   * @return specification to filter by core
   */
  public static Specification<Wand> coreEqualsIgnoreCase(String core) {
    return (root, query, cb) -> cb.equal(cb.lower(root.get("core")), core.toLowerCase());
  }

  /**
   * Filters wands assigned to the given wizard id.
   *
   * @param wizardId wizard id
   * @return specification to filter by assigned wizard id
   */
  public static Specification<Wand> assignedToWizard(Long wizardId) {
    return (root, query, cb) -> cb.equal(root.get("assignedWizardId"), wizardId);
  }

  /**
   * Filters wands by assignment status.
   *
   * @param assigned whether the wand is assigned
   * @return specification to filter by assignment status
   */
  public static Specification<Wand> isAssigned(boolean assigned) {
    return (root, query, cb) ->
        assigned
            ? cb.isNotNull(root.get("assignedWizardId"))
            : cb.isNull(root.get("assignedWizardId"));
  }
}
