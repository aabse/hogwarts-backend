package com.magic.hogwarts.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.magic.hogwarts.inventory.api.dto.request.BroomCreateRequestDTO;
import com.magic.hogwarts.inventory.api.dto.response.BroomDTO;
import com.magic.hogwarts.inventory.domain.model.Broom;
import com.magic.hogwarts.inventory.infrastructure.persistence.BroomRepository;
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
class BroomServiceTest {

  /** Repository mock used by the service under test. */
  @Mock private BroomRepository repository;

  /** Service under test. */
  private BroomService service;

  @BeforeEach
  void setUp() {
    service = new BroomService(repository);
  }

  @Test
  void create_mapsFieldsAndReturnsDto() {
    BroomCreateRequestDTO req = new BroomCreateRequestDTO();
    req.setModel("Nimbus 2000");
    req.setSpeedRating(10);
    req.setQuantity(3);

    when(repository.save(any(Broom.class)))
        .thenAnswer(
            inv -> {
              Broom b = inv.getArgument(0);
              b.setId(1L);
              return b;
            });

    BroomDTO dto = service.create(req);

    assertThat(dto.getId()).isEqualTo(1L);
    assertThat(dto.getModel()).isEqualTo("Nimbus 2000");
    assertThat(dto.getSpeedRating()).isEqualTo(10);
    assertThat(dto.getQuantity()).isEqualTo(3);
  }

  @Test
  void list_whenNoFilters_usesFindAllPageable() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(pageable)).thenReturn(Page.empty());

    service.list(null, null, null, pageable);

    verify(repository).findAll(pageable);
  }

  @Test
  void list_whenAnyFilterProvided_usesFindAllSpecification() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

    service.list("Nimbus", null, null, pageable);

    verify(repository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void list_whenAllFiltersProvided_buildsSpecificationAndUsesFindAllSpecification() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

    service.list("Nimbus", 1, 0, pageable);

    verify(repository).findAll(any(Specification.class), eq(pageable));
  }
}
