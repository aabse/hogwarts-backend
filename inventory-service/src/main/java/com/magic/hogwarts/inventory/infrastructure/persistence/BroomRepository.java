package com.magic.hogwarts.inventory.infrastructure.persistence;

import com.magic.hogwarts.inventory.domain.model.Broom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository for {@link Broom} entities.
 *
 * @author Angel Absé
 * @since 1.0
 */
public interface BroomRepository
    extends JpaRepository<Broom, Long>, JpaSpecificationExecutor<Broom> {}
