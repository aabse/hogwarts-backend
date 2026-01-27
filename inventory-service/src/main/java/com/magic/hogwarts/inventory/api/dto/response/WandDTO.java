package com.magic.hogwarts.inventory.api.dto.response;

import lombok.Data;

/**
 * Wand representation returned by the API.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class WandDTO {
  /** Wand identifier. */
  private Long id;

  /** Wand wood type. */
  private String wood;

  /** Wand core. */
  private String core;

  /** Wand length (inches). */
  private Double length;

  /** Optional description. */
  private String description;

  /** Wizard id assigned to this wand (nullable). */
  private Long assignedWizardId;
}
