package com.magic.hogwarts.duel.infrastructure.persistence;

import com.magic.hogwarts.duel.domain.model.Duel;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link Duel} entities.
 *
 * @author Angel Absé
 * @since 1.0
 */
public interface DuelRepository extends JpaRepository<Duel, Long> {}
