package com.magic.hogwarts.wizard.infrastructure.persistence;

import com.magic.hogwarts.wizard.domain.model.Wizard;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory for reusable {@link Specification} objects for {@link Wizard} queries.
 *
 * @author Angel Absé
 * @since 1.0
 */
public final class WizardSpecifications {
  /** Utility class. */
  private WizardSpecifications() {}

  /**
   * Filters by exact house (case-insensitive).
   *
   * @param house house name
   * @return specification to filter by house
   */
  public static Specification<Wizard> houseEqualsIgnoreCase(String house) {
    return (root, query, cb) -> cb.equal(cb.lower(root.get("house")), house.toLowerCase());
  }

  /**
   * Filters by name containing a substring (case-insensitive).
   *
   * @param name name substring
   * @return specification to filter by name substring
   */
  public static Specification<Wizard> nameContainsIgnoreCase(String name) {
    String like = "%" + name.toLowerCase() + "%";
    return (root, query, cb) -> cb.like(cb.lower(root.get("name")), like);
  }
}
