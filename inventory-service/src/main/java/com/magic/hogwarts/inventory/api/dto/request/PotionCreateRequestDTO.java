package com.magic.hogwarts.inventory.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload to create a new potion.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class PotionCreateRequestDTO {

  /** Potion name. */
  @NotBlank(message = "Potion name is required") @Size(min = 2, max = 80, message = "Potion name must be between 2 and 80 characters long") private String name;

  /** Optional potion effect description. */
  @Size(max = 255, message = "Effect must not exceed 255 characters") private String effect;

  /** Initial quantity in stock. */
  @NotNull(message = "Quantity is required") @Min(value = 0, message = "Quantity must not be negative") private Integer quantity;
}
