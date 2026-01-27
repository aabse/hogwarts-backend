package com.magic.hogwarts.inventory.application;

import com.magic.hogwarts.inventory.api.dto.request.PotionCreateRequestDTO;
import com.magic.hogwarts.inventory.api.dto.response.PotionDTO;
import com.magic.hogwarts.inventory.domain.model.Potion;
import com.magic.hogwarts.inventory.infrastructure.persistence.PotionRepository;
import com.magic.hogwarts.inventory.infrastructure.persistence.PotionSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Application service implementing potion inventory use-cases.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Service
public class PotionService {

  /** Potion persistence gateway. */
  private final PotionRepository repository;

  /**
   * Creates the service.
   *
   * @param repository potion repository
   */
  public PotionService(PotionRepository repository) {
    this.repository = repository;
  }

  /**
   * Creates a new potion in inventory.
   *
   * @param request creation request payload
   * @return created potion representation
   */
  public PotionDTO create(PotionCreateRequestDTO request) {
    Potion potion = new Potion();
    potion.setName(request.getName());
    potion.setEffect(request.getEffect());
    potion.setQuantity(request.getQuantity());
    return toDTO(repository.save(potion));
  }

  /**
   * Lists potions using optional filters and pagination.
   *
   * @param name optional name filter (contains, case-insensitive)
   * @param effect optional effect filter (contains, case-insensitive)
   * @param minQuantity optional minimum quantity filter
   * @param pageable paging/sorting information
   * @return a page of matching potions
   */
  public Page<PotionDTO> list(String name, String effect, Integer minQuantity, Pageable pageable) {
    Specification<Potion> spec = null;

    if (name != null && !name.isBlank()) {
      spec = PotionSpecifications.nameContainsIgnoreCase(name);
    }
    if (effect != null && !effect.isBlank()) {
      Specification<Potion> effectSpec = PotionSpecifications.effectContainsIgnoreCase(effect);
      spec = (spec == null) ? effectSpec : spec.and(effectSpec);
    }
    if (minQuantity != null) {
      Specification<Potion> qSpec = PotionSpecifications.quantityGte(minQuantity);
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
   * @param p potion entity
   * @return API response DTO
   */
  private PotionDTO toDTO(Potion p) {
    PotionDTO dto = new PotionDTO();
    dto.setId(p.getId());
    dto.setName(p.getName());
    dto.setEffect(p.getEffect());
    dto.setQuantity(p.getQuantity());
    return dto;
  }
}
