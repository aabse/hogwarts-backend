package com.magic.hogwarts.inventory.application;

import com.magic.hogwarts.inventory.api.dto.request.BroomCreateRequestDTO;
import com.magic.hogwarts.inventory.api.dto.response.BroomDTO;
import com.magic.hogwarts.inventory.domain.model.Broom;
import com.magic.hogwarts.inventory.infrastructure.persistence.BroomRepository;
import com.magic.hogwarts.inventory.infrastructure.persistence.BroomSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Application service implementing broom inventory use-cases.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Service
public class BroomService {

  /** Broom persistence gateway. */
  private final BroomRepository repository;

  /**
   * Creates the service.
   *
   * @param repository broom repository
   */
  public BroomService(BroomRepository repository) {
    this.repository = repository;
  }

  /**
   * Creates a new broom in inventory.
   *
   * @param request creation request payload
   * @return created broom representation
   */
  public BroomDTO create(BroomCreateRequestDTO request) {
    Broom broom = new Broom();
    broom.setModel(request.getModel());
    broom.setSpeedRating(request.getSpeedRating());
    broom.setQuantity(request.getQuantity());
    return toDTO(repository.save(broom));
  }

  /**
   * Lists brooms using optional filters and pagination.
   *
   * @param model optional model filter (contains, case-insensitive)
   * @param minSpeed optional minimum speed rating
   * @param minQuantity optional minimum quantity
   * @param pageable paging/sorting information
   * @return a page of matching brooms
   */
  public Page<BroomDTO> list(
      String model, Integer minSpeed, Integer minQuantity, Pageable pageable) {
    Specification<Broom> spec = null;

    if (model != null && !model.isBlank()) {
      spec = BroomSpecifications.modelContainsIgnoreCase(model);
    }
    if (minSpeed != null) {
      Specification<Broom> speedSpec = BroomSpecifications.speedGte(minSpeed);
      spec = (spec == null) ? speedSpec : spec.and(speedSpec);
    }
    if (minQuantity != null) {
      Specification<Broom> qSpec = BroomSpecifications.quantityGte(minQuantity);
      spec = (spec == null) ? qSpec : spec.and(qSpec);
    }

    if (spec == null) {
      return repository.findAll(pageable).map(this::toDTO);
    }

    return repository.findAll(spec, pageable).map(this::toDTO);
  }

  /**
   * Maps the entity to the public API DTO.
   *
   * @param b broom entity
   * @return API response DTO
   */
  private BroomDTO toDTO(Broom b) {
    BroomDTO dto = new BroomDTO();
    dto.setId(b.getId());
    dto.setModel(b.getModel());
    dto.setSpeedRating(b.getSpeedRating());
    dto.setQuantity(b.getQuantity());
    return dto;
  }
}
