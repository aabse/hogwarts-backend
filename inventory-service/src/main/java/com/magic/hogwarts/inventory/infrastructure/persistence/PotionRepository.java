package com.magic.hogwarts.inventory.infrastructure.persistence;

import com.magic.hogwarts.inventory.domain.model.Potion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository for {@link Potion} entities.
 *
 * @author Angel Absé
 * @since 1.0
 */
public interface PotionRepository
    extends JpaRepository<Potion, Long>, JpaSpecificationExecutor<Potion> {}
