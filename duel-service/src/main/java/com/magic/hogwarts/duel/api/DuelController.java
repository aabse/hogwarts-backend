package com.magic.hogwarts.duel.api;

import com.magic.hogwarts.duel.api.dto.request.CreateDuelRequestDTO;
import com.magic.hogwarts.duel.api.dto.response.DuelDTO;
import com.magic.hogwarts.duel.application.DuelService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for duel operations.
 *
 * @author Angel Absé
 * @since 1.0
 */
@RestController
@RequestMapping("/api/duels")
public class DuelController {

  /** Application service containing duel use-cases. */
  private final DuelService service;

  /**
   * Creates the controller.
   *
   * @param service duel application service
   */
  public DuelController(DuelService service) {
    this.service = service;
  }

  /**
   * Creates a duel record and returns the resolved outcome.
   *
   * @param request duel creation payload
   * @return duel representation including winner/loser
   */
  @PostMapping
  public DuelDTO create(@Valid @RequestBody CreateDuelRequestDTO request) {
    return service.create(request);
  }

  /**
   * Retrieves a duel by id.
   *
   * @param id duel id
   * @return duel representation
   */
  @GetMapping("/{id}")
  public DuelDTO getById(@PathVariable Long id) {
    return service.getById(id);
  }
}
