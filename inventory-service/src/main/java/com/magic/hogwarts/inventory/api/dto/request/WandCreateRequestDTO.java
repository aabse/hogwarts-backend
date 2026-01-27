package com.magic.hogwarts.inventory.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload to create a new wand.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class WandCreateRequestDTO {

  /** Wand wood type. */
  @NotBlank(message = "Wood is required") @Size(min = 2, max = 60, message = "Wood must be between 2 and 60 characters long") private String wood;

  /** Wand core. */
  @NotBlank(message = "Core is required") @Size(min = 2, max = 60, message = "Core must be between 2 and 60 characters long") private String core;

  /** Wand length (inches). */
  @NotNull(message = "Length is required") @Positive(message = "Length must be positive") private Double length;

  /** Optional human-friendly description. */
  @Size(max = 255, message = "Description must not exceed 255 characters") private String description;
}
