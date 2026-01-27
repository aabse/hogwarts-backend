package com.magic.hogwarts.inventory.infrastructure.persistence;

import com.magic.hogwarts.inventory.domain.model.Broom;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory for reusable {@link Specification} objects for {@link Broom} queries.
 *
 * @author Angel Absé
 * @since 1.0
 */
public final class BroomSpecifications {
  /** Utility class. */
  private BroomSpecifications() {}

  /**
   * Filters by model containing a substring (case-insensitive).
   *
   * @param model model substring
   * @return specification to filter by model substring
   */
  public static Specification<Broom> modelContainsIgnoreCase(String model) {
    String like = "%" + model.toLowerCase() + "%";
    return (root, query, cb) -> cb.like(cb.lower(root.get("model")), like);
  }

  /**
   * Filters by minimum speed rating.
   *
   * @param minSpeed minimum speed (inclusive)
   * @return specification to filter by minimum speed
   */
  public static Specification<Broom> speedGte(Integer minSpeed) {
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("speedRating"), minSpeed);
  }

  /**
   * Filters by minimum quantity.
   *
   * @param minQuantity minimum quantity (inclusive)
   * @return specification to filter by minimum quantity
   */
  public static Specification<Broom> quantityGte(Integer minQuantity) {
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("quantity"), minQuantity);
  }
}
