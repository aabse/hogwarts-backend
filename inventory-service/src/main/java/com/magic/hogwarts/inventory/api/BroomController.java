package com.magic.hogwarts.inventory.api;

import com.magic.hogwarts.inventory.api.dto.request.BroomCreateRequestDTO;
import com.magic.hogwarts.inventory.api.dto.response.BroomDTO;
import com.magic.hogwarts.inventory.application.BroomService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for broom inventory operations.
 *
 * @author Angel Absé
 * @since 1.0
 */
@RestController
@RequestMapping("/api/inventory/brooms")
public class BroomController {

  /** Application service containing broom use-cases. */
  private final BroomService service;

  /**
   * Creates the controller.
   *
   * @param service broom application service
   */
  public BroomController(BroomService service) {
    this.service = service;
  }

  /**
   * Creates a new broom in inventory.
   *
   * @param request creation request payload
   * @return created broom representation
   */
  @PostMapping
  public BroomDTO create(@Valid @RequestBody BroomCreateRequestDTO request) {
    return service.create(request);
  }

  /**
   * Lists brooms with optional filters and pagination.
   *
   * @param model optional model filter (contains, case-insensitive)
   * @param minSpeed optional minimum speed rating
   * @param minQuantity optional minimum quantity
   * @param pageable paging and sorting information
   * @return a page of broom DTOs
   */
  @GetMapping
  public Page<BroomDTO> list(
      @RequestParam(required = false) String model,
      @RequestParam(required = false) Integer minSpeed,
      @RequestParam(required = false) Integer minQuantity,
      @PageableDefault(size = 20) Pageable pageable) {
    return service.list(model, minSpeed, minQuantity, pageable);
  }
}
