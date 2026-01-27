package com.magic.hogwarts.inventory.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload to create a new broom.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class BroomCreateRequestDTO {

  /** Broom model. */
  @NotBlank(message = "Broom model is required") @Size(min = 2, max = 80, message = "Broom model must be between 2 and 80 characters long") private String model;

  /** Speed rating (higher is faster). */
  @NotNull(message = "speedRating is required") @Positive(message = "speedRating must be positive") private Integer speedRating;

  /** Quantity in stock. */
  @NotNull(message = "Quantity is required") @Min(value = 0, message = "Quantity must not be negative") private Integer quantity;
}
