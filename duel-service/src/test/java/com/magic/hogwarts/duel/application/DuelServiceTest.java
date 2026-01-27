package com.magic.hogwarts.duel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.magic.hogwarts.duel.api.dto.request.CreateDuelRequestDTO;
import com.magic.hogwarts.duel.api.dto.response.DuelDTO;
import com.magic.hogwarts.duel.domain.model.Duel;
import com.magic.hogwarts.duel.domain.model.DuelStatus;
import com.magic.hogwarts.duel.infrastructure.persistence.DuelRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuelServiceTest {

  /** Repository mock used by the service under test. */
  @Mock private DuelRepository repository;

  /** Captures persisted duel entities. */
  @Captor private ArgumentCaptor<Duel> duelCaptor;

  /** Service under test. */
  private DuelService service;

  @BeforeEach
  void setUp() {
    service = new DuelService(repository);
  }

  @Test
  void create_whenIdsEqual_throwsIllegalArgumentExceptionWithEnglishMessage() {
    CreateDuelRequestDTO req = new CreateDuelRequestDTO();
    req.setAttackerId(1L);
    req.setDefenderId(1L);

    assertThatThrownBy(() -> service.create(req))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("attackerId and defenderId cannot be the same");
  }

  @Test
  void getById_whenMissing_throwsNoSuchElementExceptionWithEnglishMessage() {
    when(repository.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(5L))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("Duel not found");
  }

  @Test
  void create_success_setsFinishedStatusAndMessageAndWinnerLoserConsistency() {
    CreateDuelRequestDTO req = new CreateDuelRequestDTO();
    req.setAttackerId(1L);
    req.setDefenderId(2L);

    when(repository.save(any(Duel.class)))
        .thenAnswer(
            inv -> {
              Duel d = inv.getArgument(0);
              d.setId(10L);
              return d;
            });

    DuelDTO dto = service.create(req);

    assertThat(dto.getId()).isEqualTo(10L);
    assertThat(dto.getStatus()).isEqualTo(DuelStatus.FINISHED);
    assertThat(dto.getMessage()).isEqualTo("Duel completed");
    assertThat(dto.getAttackerId()).isEqualTo(1L);
    assertThat(dto.getDefenderId()).isEqualTo(2L);
    assertThat(dto.getCreatedAt()).isNotNull();
    assertThat(dto.getFinishedAt()).isNotNull();
    assertThat(dto.getDebugSeed()).isNotNull();

    assertThat(dto.getWinnerId()).isIn(1L, 2L);
    assertThat(dto.getLoserId()).isIn(1L, 2L);
    assertThat(dto.getWinnerId()).isNotEqualTo(dto.getLoserId());
  }

  @Test
  void getById_whenPresent_mapsToDto() {
    Duel duel = new Duel();
    duel.setId(5L);
    duel.setAttackerId(1L);
    duel.setDefenderId(2L);
    duel.setStatus(DuelStatus.FINISHED);
    duel.setWinnerId(1L);
    duel.setLoserId(2L);
    duel.setMessage("Duel completed");

    when(repository.findById(5L)).thenReturn(Optional.of(duel));

    DuelDTO dto = service.getById(5L);

    assertThat(dto.getId()).isEqualTo(5L);
    assertThat(dto.getWinnerId()).isEqualTo(1L);
    assertThat(dto.getMessage()).isEqualTo("Duel completed");
  }
}
