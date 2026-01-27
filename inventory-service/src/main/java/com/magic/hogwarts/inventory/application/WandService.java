package com.magic.hogwarts.inventory.application;

import com.magic.hogwarts.inventory.api.dto.request.AssignWandRequestDTO;
import com.magic.hogwarts.inventory.api.dto.request.WandCreateRequestDTO;
import com.magic.hogwarts.inventory.api.dto.response.WandDTO;
import com.magic.hogwarts.inventory.domain.model.Wand;
import com.magic.hogwarts.inventory.infrastructure.persistence.WandRepository;
import com.magic.hogwarts.inventory.infrastructure.persistence.WandSpecifications;
import jakarta.transaction.Transactional;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Application service implementing wand inventory use-cases.
 *
 * @author Angel Absé
 * @since 1.0
 */
@Service
public class WandService {

  /** Wand persistence gateway. */
  private final WandRepository repository;

  /**
   * Creates the service.
   *
   * @param repository wand repository
   */
  public WandService(WandRepository repository) {
    this.repository = repository;
  }

  /**
   * Creates a new wand in inventory.
   *
   * @param request creation request payload
   * @return created wand representation
   */
  public WandDTO create(WandCreateRequestDTO request) {
    Wand wand = new Wand();
    wand.setWood(request.getWood());
    wand.setCore(request.getCore());
    wand.setLength(request.getLength());
    wand.setDescription(request.getDescription());
    wand.setAssignedWizardId(null);

    Wand saved = repository.save(wand);
    return toDTO(saved);
  }

  /**
   * Lists wands using optional filters and pagination.
   *
   * @param wood optional wood filter (exact match, case-insensitive)
   * @param core optional core filter (exact match, case-insensitive)
   * @param assigned optional assigned filter (true/false)
   * @param wizardId optional wizard id filter for assigned wands
   * @param pageable paging/sorting information
   * @return a page of matching wands
   */
  public Page<WandDTO> list(
      String wood, String core, Boolean assigned, Long wizardId, Pageable pageable) {
    Specification<Wand> spec = null;

    if (wood != null && !wood.isBlank()) {
      spec = WandSpecifications.woodEqualsIgnoreCase(wood);
    }
    if (core != null && !core.isBlank()) {
      Specification<Wand> coreSpec = WandSpecifications.coreEqualsIgnoreCase(core);
      spec = (spec == null) ? coreSpec : spec.and(coreSpec);
    }
    if (assigned != null) {
      Specification<Wand> assignedSpec = WandSpecifications.isAssigned(assigned);
      spec = (spec == null) ? assignedSpec : spec.and(assignedSpec);
    }
    if (wizardId != null) {
      Specification<Wand> wizardSpec = WandSpecifications.assignedToWizard(wizardId);
      spec = (spec == null) ? wizardSpec : spec.and(wizardSpec);
    }

    if (spec == null) {
      return repository.findAll(pageable).map(this::toDTO);
    }

    return repository.findAll(spec, pageable).map(this::toDTO);
  }

  /**
   * Assigns a wand to a wizard by id.
   *
   * <p>Rule: if a wand is already assigned to a different wizard, the operation fails with a
   * conflict.
   *
   * @param wandId wand id
   * @param request assignment request containing wizard id
   * @return updated wand representation
   * @throws NoSuchElementException if the wand does not exist
   * @throws IllegalStateException if the wand is already assigned to another wizard
   */
  @Transactional
  public WandDTO assign(Long wandId, AssignWandRequestDTO request) {
    Wand wand =
        repository.findById(wandId).orElseThrow(() -> new NoSuchElementException("Wand not found"));

    Long current = wand.getAssignedWizardId();
    Long target = request.getWizardId();

    if (current != null && !current.equals(target)) {
      throw new IllegalStateException("The wand is already assigned to another wizard");
    }

    wand.setAssignedWizardId(target);
    return toDTO(repository.save(wand));
  }

  /**
   * Maps the entity to the public API DTO.
   *
   * @param w wand entity
   * @return API response DTO
   */
  private WandDTO toDTO(Wand w) {
    WandDTO dto = new WandDTO();
    dto.setId(w.getId());
    dto.setWood(w.getWood());
    dto.setCore(w.getCore());
    dto.setLength(w.getLength());
    dto.setDescription(w.getDescription());
    dto.setAssignedWizardId(w.getAssignedWizardId());
    return dto;
  }
}
