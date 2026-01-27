package com.magic.hogwarts.wizard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.magic.hogwarts.wizard.api.dto.request.WizardPatchRequestDTO;
import com.magic.hogwarts.wizard.api.dto.request.WizardRequestDTO;
import com.magic.hogwarts.wizard.api.dto.response.AttackResultDTO;
import com.magic.hogwarts.wizard.api.dto.response.WizardDTO;
import com.magic.hogwarts.wizard.domain.model.Wizard;
import com.magic.hogwarts.wizard.infrastructure.persistence.WizardRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class WizardServiceTest {

  /** Repository mock used by the service under test. */
  @Mock private WizardRepository repository;

  /** Captures persisted wizard entities. */
  @Captor private ArgumentCaptor<Wizard> wizardCaptor;

  /** Service under test. */
  private WizardService service;

  @BeforeEach
  void setUp() {
    service = new WizardService(repository);
  }

  @Test
  void createWizard_whenHouseBlank_assignsRandomHouseFromAllowedSet() {
    WizardRequestDTO request = new WizardRequestDTO();
    request.setName("Harry Potter");
    request.setHouse("   ");

    when(repository.existsByHouseIgnoreCaseAndNameIgnoreCase(anyString(), anyString()))
        .thenReturn(false);
    when(repository.save(any(Wizard.class)))
        .thenAnswer(
            inv -> {
              Wizard w = inv.getArgument(0);
              w.setId(1L);
              return w;
            });

    WizardDTO dto = service.createWizard(request);

    verify(repository).save(wizardCaptor.capture());
    Wizard saved = wizardCaptor.getValue();
    assertThat(saved.getHouse()).isNotBlank();
    assertThat(saved.getHouse()).isIn("Gryffindor", "Hufflepuff", "Ravenclaw", "Slytherin");
    assertThat(dto.getId()).isEqualTo(1L);
  }

  @Test
  void createWizard_whenWizardAlreadyExists_throwsIllegalArgumentExceptionWithEnglishMessage() {
    WizardRequestDTO request = new WizardRequestDTO();
    request.setName("Harry Potter");
    request.setHouse("Gryffindor");

    when(repository.existsByHouseIgnoreCaseAndNameIgnoreCase("Gryffindor", "Harry Potter"))
        .thenReturn(true);

    assertThatThrownBy(() -> service.createWizard(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("A wizard with that name already exists in that house");

    verify(repository, never()).save(any(Wizard.class));
  }

  @Test
  void getById_whenMissing_throwsNoSuchElementExceptionWithEnglishMessage() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(99L))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("Wizard not found");
  }

  @Test
  void patch_whenNoFieldsProvided_returnsExistingWizardWithoutSaving() {
    Wizard existing = new Wizard();
    existing.setId(10L);
    existing.setName("Hermione");
    existing.setHouse("Gryffindor");
    existing.setWandId(123L);

    when(repository.findById(10L)).thenReturn(Optional.of(existing));

    WizardPatchRequestDTO req = new WizardPatchRequestDTO();
    WizardDTO dto = service.patch(10L, req);

    assertThat(dto.getId()).isEqualTo(10L);
    assertThat(dto.getName()).isEqualTo("Hermione");
    assertThat(dto.getWandDescription()).isEqualTo("Wand assigned");
    verify(repository, never()).save(any(Wizard.class));
  }

  @Test
  void delete_whenMissing_throwsNoSuchElementExceptionWithEnglishMessage() {
    when(repository.existsById(7L)).thenReturn(false);

    assertThatThrownBy(() -> service.delete(7L))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("Wizard not found");
  }

  @Test
  void attack_whenIdsMissing_throwsIllegalArgumentExceptionWithEnglishMessage() {
    assertThatThrownBy(() -> service.attack(null, 2L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("attackerId and defenderId are required");
  }

  @Test
  void attack_whenIdsEqual_throwsIllegalArgumentExceptionWithEnglishMessage() {
    assertThatThrownBy(() -> service.attack(1L, 1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Attacker and defender cannot be the same wizard");
  }

  @Test
  void attack_success_isDeterministicWhenLevelsDiffer() {
    Wizard attacker = new Wizard();
    attacker.setId(1L);
    attacker.setLevel(5);

    Wizard defender = new Wizard();
    defender.setId(2L);
    defender.setLevel(3);

    when(repository.findById(1L)).thenReturn(Optional.of(attacker));
    when(repository.findById(2L)).thenReturn(Optional.of(defender));
    when(repository.save(any(Wizard.class))).thenAnswer(inv -> inv.getArgument(0));

    AttackResultDTO result = service.attack(1L, 2L);

    assertThat(result.getWinnerId()).isEqualTo(1L);
    assertThat(result.getLoserId()).isEqualTo(2L);
    assertThat(result.getWinnerLevelBefore()).isEqualTo(5);
    assertThat(result.getWinnerLevelAfter()).isEqualTo(6);
    assertThat(result.getLoserLevelBefore()).isEqualTo(3);
    assertThat(result.getLoserLevelAfter()).isEqualTo(2);
    assertThat(result.getMessage()).isEqualTo("Duel completed");

    verify(repository).save(attacker);
    verify(repository).save(defender);
  }

  @Test
  void attack_loserLevelNeverGoesBelowOne() {
    Wizard attacker = new Wizard();
    attacker.setId(1L);
    attacker.setLevel(10);

    Wizard defender = new Wizard();
    defender.setId(2L);
    defender.setLevel(1);

    when(repository.findById(1L)).thenReturn(Optional.of(attacker));
    when(repository.findById(2L)).thenReturn(Optional.of(defender));
    when(repository.save(any(Wizard.class))).thenAnswer(inv -> inv.getArgument(0));

    AttackResultDTO result = service.attack(1L, 2L);

    assertThat(result.getLoserLevelAfter()).isEqualTo(1);
    assertThat(defender.getLevel()).isEqualTo(1);
  }

  @Test
  void attack_whenAttackerMissing_throwsNoSuchElementExceptionWithEnglishMessage() {
    when(repository.findById(anyLong())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.attack(1L, 2L))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("Wizard not found");
  }

  @Test
  void list_whenNoFilters_usesFindAllPageable() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(pageable)).thenReturn(Page.empty());

    service.list(null, null, pageable);

    verify(repository).findAll(pageable);
  }

  @Test
  void list_whenAnyFilterProvided_usesFindAllSpecification() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

    service.list("Gryffindor", null, pageable);

    verify(repository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void list_whenBothFiltersProvided_buildsSpecificationAndUsesFindAllSpecification() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

    service.list("Gryffindor", "har", pageable);

    verify(repository).findAll(any(Specification.class), eq(pageable));
  }
}
