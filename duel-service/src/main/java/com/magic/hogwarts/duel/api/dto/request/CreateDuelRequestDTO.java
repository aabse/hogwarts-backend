package com.magic.hogwarts.duel.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Request payload to create a duel.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Data
public class CreateDuelRequestDTO {

  /** Attacker wizard id. */
  @NotNull(message = "attackerId is required") @Positive(message = "attackerId must be a positive number") private Long attackerId;

  /** Defender wizard id. */
  @NotNull(message = "defenderId is required") @Positive(message = "defenderId must be a positive number") private Long defenderId;
}
