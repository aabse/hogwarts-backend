package com.magic.hogwarts.inventory.api.dto.response;

import lombok.Data;

/**
 * Broom representation returned by the API.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class BroomDTO {
  /** Broom identifier. */
  private Long id;

  /** Broom model. */
  private String model;

  /** Speed rating (higher is faster). */
  private Integer speedRating;

  /** Quantity in stock. */
  private Integer quantity;
}
