package com.magic.hogwarts.wizard.api.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload to partially update a wizard.
 *
 * <p>All fields are optional; only non-null values are applied.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class WizardPatchRequestDTO {
  /** New wizard name (optional). */
  @Size(min = 3, message = "Wizard name must be at least 3 characters long") private String name;

  /** New wizard house (optional). */
  @Pattern(
      regexp = "^(Gryffindor|Slytherin|Ravenclaw|Hufflepuff)$",
      message = "House must be one of Hogwarts' 4 official houses")
  private String house;
}
