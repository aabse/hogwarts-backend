package com.magic.hogwarts.inventory.infrastructure.persistence;

import com.magic.hogwarts.inventory.domain.model.Wand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository for {@link Wand} entities.
 *
 * @author Angel Absé
 * @since 1.0
 */
public interface WandRepository extends JpaRepository<Wand, Long>, JpaSpecificationExecutor<Wand> {}
