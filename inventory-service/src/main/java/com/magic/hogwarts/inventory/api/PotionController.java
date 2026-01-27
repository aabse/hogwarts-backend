package com.magic.hogwarts.inventory.api;

import com.magic.hogwarts.inventory.api.dto.request.PotionCreateRequestDTO;
import com.magic.hogwarts.inventory.api.dto.response.PotionDTO;
import com.magic.hogwarts.inventory.application.PotionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for potion inventory operations.
 *
 * @author Angel Absé
 * @since 1.0
 */
@RestController
@RequestMapping("/api/inventory/potions")
public class PotionController {

  /** Application service containing potion use-cases. */
  private final PotionService service;

  /**
   * Creates the controller.
   *
   * @param service potion application service
   */
  public PotionController(PotionService service) {
    this.service = service;
  }

  /**
   * Creates a new potion in inventory.
   *
   * @param request creation request payload
   * @return created potion representation
   */
  @PostMapping
  public PotionDTO create(@Valid @RequestBody PotionCreateRequestDTO request) {
    return service.create(request);
  }

  /**
   * Lists potions with optional filters and pagination.
   *
   * @param name optional name filter (contains, case-insensitive)
   * @param effect optional effect filter (contains, case-insensitive)
   * @param minQuantity optional minimum quantity filter
   * @param pageable paging and sorting information
   * @return a page of potion DTOs
   */
  @GetMapping
  public Page<PotionDTO> list(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String effect,
      @RequestParam(required = false) Integer minQuantity,
      @PageableDefault(size = 20) Pageable pageable) {
    return service.list(name, effect, minQuantity, pageable);
  }
}
