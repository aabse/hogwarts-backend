package com.magic.hogwarts.inventory.domain.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Potion JPA entity (write model) owned by the inventory service.
 *
 * <p>Note: controllers should never expose this entity directly; they should return DTOs.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Entity
@Data
public class Potion {
  /** Database identifier. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Potion name. */
  private String name;

  /** Optional effect description. */
  private String effect;

  /** Quantity in stock. */
  private Integer quantity;

  /** Optimistic locking version column. */
  @Version private Long version;
}
