package com.magic.hogwarts.inventory.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Request payload to assign a wand to a wizard.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class AssignWandRequestDTO {
  /** Wizard id to assign the wand to. */
  @NotNull(message = "wizardId is required") @Positive(message = "wizardId must be a positive number") private Long wizardId;
}
