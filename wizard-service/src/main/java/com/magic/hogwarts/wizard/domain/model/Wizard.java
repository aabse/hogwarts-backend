package com.magic.hogwarts.wizard.domain.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Wizard JPA entity (write model) owned by the wizard service.
 *
 * <p>Note: controllers should never expose this entity directly; they should return DTOs.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Entity
@Data
public class Wizard {
  /** Database identifier. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Wizard name. */
  private String name;

  /** Hogwarts house. */
  private String house;

  /** Wizard patronus (currently optional / not enforced). */
  private String patronus;

  /** Wizard level (defaults to 1). */
  private Integer level = 1;

  /**
   * Boundary (microservices): the wizard-service should not depend on the wand domain model. We
   * store only the wand id (the description can be resolved later via integration/events).
   */
  private Long wandId;

  /** Optimistic locking version column. */
  @Version private Long version;
}
