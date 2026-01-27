package com.magic.hogwarts.wizard.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload to create a new wizard.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class WizardRequestDTO {

  /** Wizard name. */
  @NotBlank(message = "Wizard name is required") @Size(min = 3, message = "Wizard name must be at least 3 characters long") private String name;

  /** Optional house. If blank, the service may assign one automatically. */
  @Pattern(
      regexp = "^\\s*$|^(Gryffindor|Slytherin|Ravenclaw|Hufflepuff)$",
      message =
          "House must be one of Hogwarts' 4 official houses (or blank so the Sorting Hat can decide)")
  private String house;
}
