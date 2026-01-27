package com.magic.hogwarts.inventory.domain.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Wand JPA entity (write model) owned by the inventory service.
 *
 * <p>Note: controllers should never expose this entity directly; they should return DTOs.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Entity
@Data
public class Wand {
  /** Database identifier. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Wand wood type. */
  private String wood;

  /** Wand core. */
  private String core;

  /** Wand length (inches). */
  private Double length;

  /** Optional human-friendly description. */
  private String description;

  /**
   * Boundary (microservices): inventory-service does not depend on the wizard domain model. We
   * store only the assigned wizard id.
   */
  private Long assignedWizardId;

  /** Optimistic locking version column. */
  @Version private Long version;
}
