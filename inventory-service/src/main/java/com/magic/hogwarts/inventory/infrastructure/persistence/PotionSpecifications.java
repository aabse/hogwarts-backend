package com.magic.hogwarts.inventory.infrastructure.persistence;

import com.magic.hogwarts.inventory.domain.model.Potion;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory for reusable {@link Specification} objects for {@link Potion} queries.
 *
 * @author Angel Absé
 * @since 1.0
 */
public final class PotionSpecifications {
  /** Utility class. */
  private PotionSpecifications() {}

  /**
   * Filters by name containing a substring (case-insensitive).
   *
   * @param name name substring
   * @return specification to filter by name
   */
  public static Specification<Potion> nameContainsIgnoreCase(String name) {
    String like = "%" + name.toLowerCase() + "%";
    return (root, query, cb) -> cb.like(cb.lower(root.get("name")), like);
  }

  /**
   * Filters by effect containing a substring (case-insensitive).
   *
   * @param effect effect substring
   * @return specification to filter by effect
   */
  public static Specification<Potion> effectContainsIgnoreCase(String effect) {
    String like = "%" + effect.toLowerCase() + "%";
    return (root, query, cb) -> cb.like(cb.lower(root.get("effect")), like);
  }

  /**
   * Filters by minimum quantity.
   *
   * @param minQuantity minimum quantity (inclusive)
   * @return specification to filter by minimum quantity
   */
  public static Specification<Potion> quantityGte(Integer minQuantity) {
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("quantity"), minQuantity);
  }
}
