package com.magic.hogwarts.inventory.api;

import com.magic.hogwarts.inventory.api.dto.request.AssignWandRequestDTO;
import com.magic.hogwarts.inventory.api.dto.request.WandCreateRequestDTO;
import com.magic.hogwarts.inventory.api.dto.response.WandDTO;
import com.magic.hogwarts.inventory.application.WandService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for wand inventory operations.
 *
 * @author Angel Absé
 * @since 1.0
 */
@RestController
@RequestMapping("/api/inventory/wands")
public class WandController {

  /** Application service containing wand use-cases. */
  private final WandService service;

  /**
   * Creates the controller.
   *
   * @param service wand application service
   */
  public WandController(WandService service) {
    this.service = service;
  }

  /**
   * Creates a new wand in inventory.
   *
   * @param request creation request payload
   * @return created wand representation
   */
  @PostMapping
  public WandDTO create(@Valid @RequestBody WandCreateRequestDTO request) {
    return service.create(request);
  }

  /**
   * Lists wands with optional filters and pagination.
   *
   * @param wood optional wood filter (exact match, case-insensitive)
   * @param core optional core filter (exact match, case-insensitive)
   * @param assigned optional assigned filter (true/false)
   * @param wizardId optional wizard id filter for assigned wands
   * @param pageable paging and sorting information
   * @return a page of wand DTOs
   */
  @GetMapping
  public Page<WandDTO> list(
      @RequestParam(required = false) String wood,
      @RequestParam(required = false) String core,
      @RequestParam(required = false) Boolean assigned,
      @RequestParam(required = false) Long wizardId,
      @PageableDefault(size = 20) Pageable pageable) {
    return service.list(wood, core, assigned, wizardId, pageable);
  }

  /**
   * Assigns a wand to a wizard by id.
   *
   * @param wandId wand id
   * @param request assignment request containing wizard id
   * @return updated wand representation
   */
  @PostMapping("/{wandId}/assign")
  public WandDTO assign(
      @PathVariable Long wandId, @Valid @RequestBody AssignWandRequestDTO request) {
    return service.assign(wandId, request);
  }
}
