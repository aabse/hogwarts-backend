package com.magic.hogwarts.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.magic.hogwarts.inventory.api.dto.request.AssignWandRequestDTO;
import com.magic.hogwarts.inventory.api.dto.request.WandCreateRequestDTO;
import com.magic.hogwarts.inventory.api.dto.response.WandDTO;
import com.magic.hogwarts.inventory.domain.model.Wand;
import com.magic.hogwarts.inventory.infrastructure.persistence.WandRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class WandServiceTest {

  /** Repository mock used by the service under test. */
  @Mock private WandRepository repository;

  /** Service under test. */
  private WandService service;

  @BeforeEach
  void setUp() {
    service = new WandService(repository);
  }

  @Test
  void assign_whenWandMissing_throwsNoSuchElementExceptionWithEnglishMessage() {
    when(repository.findById(1L)).thenReturn(Optional.empty());

    AssignWandRequestDTO req = new AssignWandRequestDTO();
    req.setWizardId(10L);

    assertThatThrownBy(() -> service.assign(1L, req))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("Wand not found");
  }

  @Test
  void assign_whenAlreadyAssignedToDifferentWizard_throwsIllegalStateExceptionWithEnglishMessage() {
    Wand wand = new Wand();
    wand.setId(1L);
    wand.setAssignedWizardId(5L);

    when(repository.findById(1L)).thenReturn(Optional.of(wand));

    AssignWandRequestDTO req = new AssignWandRequestDTO();
    req.setWizardId(10L);

    assertThatThrownBy(() -> service.assign(1L, req))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("The wand is already assigned to another wizard");
  }

  @Test
  void assign_whenUnassigned_setsAssignedWizardIdAndReturnsDto() {
    Wand wand = new Wand();
    wand.setId(1L);
    wand.setAssignedWizardId(null);

    when(repository.findById(1L)).thenReturn(Optional.of(wand));
    when(repository.save(any(Wand.class))).thenAnswer(inv -> inv.getArgument(0));

    AssignWandRequestDTO req = new AssignWandRequestDTO();
    req.setWizardId(10L);

    WandDTO dto = service.assign(1L, req);

    assertThat(dto.getId()).isEqualTo(1L);
    assertThat(dto.getAssignedWizardId()).isEqualTo(10L);
    assertThat(wand.getAssignedWizardId()).isEqualTo(10L);
  }

  @Test
  void create_mapsFieldsAndReturnsDto() {
    WandCreateRequestDTO req = new WandCreateRequestDTO();
    req.setWood("Holly");
    req.setCore("Phoenix feather");
    req.setLength(11.0);
    req.setDescription("Highly compatible with its wizard");

    when(repository.save(any(Wand.class)))
        .thenAnswer(
            inv -> {
              Wand w = inv.getArgument(0);
              w.setId(99L);
              return w;
            });

    WandDTO dto = service.create(req);

    assertThat(dto.getId()).isEqualTo(99L);
    assertThat(dto.getWood()).isEqualTo("Holly");
    assertThat(dto.getCore()).isEqualTo("Phoenix feather");
    assertThat(dto.getLength()).isEqualTo(11.0);
    assertThat(dto.getDescription()).isEqualTo("Highly compatible with its wizard");
    assertThat(dto.getAssignedWizardId()).isNull();
  }

  @Test
  void list_whenNoFilters_usesFindAllPageable() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(pageable)).thenReturn(Page.empty());

    service.list(null, null, null, null, pageable);

    org.mockito.Mockito.verify(repository).findAll(pageable);
  }

  @Test
  void list_whenAnyFilterProvided_usesFindAllSpecification() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

    service.list("Holly", null, null, null, pageable);

    org.mockito.Mockito.verify(repository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void list_whenAllFiltersProvided_buildsSpecificationAndUsesFindAllSpecification() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

    service.list("Holly", "Phoenix feather", true, 1L, pageable);

    org.mockito.Mockito.verify(repository).findAll(any(Specification.class), eq(pageable));
  }
}
