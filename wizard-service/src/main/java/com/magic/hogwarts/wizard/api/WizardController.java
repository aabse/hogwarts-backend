package com.magic.hogwarts.wizard.api;

import com.magic.hogwarts.wizard.api.dto.request.AttackRequestDTO;
import com.magic.hogwarts.wizard.api.dto.request.WizardPatchRequestDTO;
import com.magic.hogwarts.wizard.api.dto.request.WizardRequestDTO;
import com.magic.hogwarts.wizard.api.dto.response.AttackResultDTO;
import com.magic.hogwarts.wizard.api.dto.response.WizardDTO;
import com.magic.hogwarts.wizard.application.WizardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for wizard-related operations.
 *
 * @author Angel Absé
 * @since 1.0
 */
@RestController
@RequestMapping("/api/wizards")
public class WizardController {

  /** Application service containing wizard use-cases. */
  private final WizardService service;

  /**
   * Creates the controller.
   *
   * @param service wizard application service
   */
  public WizardController(WizardService service) {
    this.service = service;
  }

  /**
   * Lists wizards with optional filters and pagination.
   *
   * @param house optional house filter (exact match, case-insensitive)
   * @param name optional name filter (contains, case-insensitive)
   * @param pageable paging and sorting information
   * @return a page of wizard DTOs
   */
  @GetMapping
  public Page<WizardDTO> getAll(
      @RequestParam(required = false) String house,
      @RequestParam(required = false) String name,
      @PageableDefault(size = 20) Pageable pageable) {
    return service.list(house, name, pageable);
  }

  /**
   * Creates a new wizard.
   *
   * @param request creation request payload
   * @return created wizard representation
   */
  @PostMapping
  public WizardDTO create(@Valid @RequestBody WizardRequestDTO request) {
    return service.createWizard(request);
  }

  /**
   * Retrieves a wizard by id.
   *
   * @param id wizard id
   * @return wizard representation
   */
  @GetMapping("/{id}")
  public WizardDTO getById(@PathVariable Long id) {
    return service.getById(id);
  }

  /**
   * Partially updates a wizard.
   *
   * @param id wizard id
   * @param request patch request payload
   * @return updated wizard representation
   */
  @PatchMapping("/{id}")
  public WizardDTO patch(@PathVariable Long id, @Valid @RequestBody WizardPatchRequestDTO request) {
    return service.patch(id, request);
  }

  /**
   * Deletes a wizard by id.
   *
   * @param id wizard id
   */
  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }

  /**
   * Runs a duel-like interaction between two wizards.
   *
   * @param request attack request payload
   * @return duel/attack result
   */
  @PostMapping("/attack")
  public AttackResultDTO attack(@Valid @RequestBody AttackRequestDTO request) {
    return service.attack(request.getAttackerId(), request.getDefenderId());
  }
}
