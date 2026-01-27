package com.magic.hogwarts.wizard.api.dto.response;

import lombok.Data;

/**
 * Wizard representation returned by the API.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class WizardDTO {
  /** Wizard identifier. */
  private Long id;

  /** Wizard name. */
  private String name;

  /** Hogwarts house. */
  private String house;

  /** Human-friendly wand description (placeholder until inventory integration exists). */
  private String wandDescription;

  /** Wizard level (starts at 1). */
  private Integer level;
}
