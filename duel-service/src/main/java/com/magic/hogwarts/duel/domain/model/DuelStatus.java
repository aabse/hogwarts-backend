package com.magic.hogwarts.duel.domain.model;

/**
 * Lifecycle status for a duel.
 *
 * @author Angel Absé
 * @since 1.0
 */
public enum DuelStatus {
  /** Duel has been created but not resolved yet. */
  CREATED,
  /** Duel has been resolved and winner/loser are available. */
  FINISHED
}
