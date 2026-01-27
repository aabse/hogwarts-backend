package com.magic.hogwarts.wizard.infrastructure.persistence;

import com.magic.hogwarts.wizard.domain.model.Wizard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository for {@link Wizard} entities.
 *
 * @author Angel Absé
 * @since 1.0
 */
public interface WizardRepository
    extends JpaRepository<Wizard, Long>, JpaSpecificationExecutor<Wizard> {
  @Override
  /**
   * Returns a page of wizards matching the given specification.
   *
   * @param spec specification (must not be {@code null})
   * @param pageable pagination information
   * @return matching page
   */
  Page<Wizard> findAll(Specification<Wizard> spec, Pageable pageable);

  /**
   * Checks uniqueness constraint for {@code (house, name)} ignoring case.
   *
   * @param house house value
   * @param name name value
   * @return {@code true} if a wizard already exists with the same house and name
   */
  boolean existsByHouseIgnoreCaseAndNameIgnoreCase(String house, String name);
}
