package com.magic.hogwarts.inventory.api.dto.response;

import lombok.Data;

/**
 * Potion representation returned by the API.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class PotionDTO {
  /** Potion identifier. */
  private Long id;

  /** Potion name. */
  private String name;

  /** Optional effect description. */
  private String effect;

  /** Quantity in stock. */
  private Integer quantity;
}
