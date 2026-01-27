package com.magic.hogwarts.wizard.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Request payload for the attack/duel operation.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class AttackRequestDTO {

  /** Attacker wizard id. */
  @NotNull(message = "Attacker id is required") @Positive(message = "Attacker id must be a positive number") private Long attackerId;

  /** Defender wizard id. */
  @NotNull(message = "Defender id is required") @Positive(message = "Defender id must be a positive number") private Long defenderId;
}
