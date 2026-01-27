package com.magic.hogwarts.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.magic.hogwarts.inventory.api.dto.request.PotionCreateRequestDTO;
import com.magic.hogwarts.inventory.api.dto.response.PotionDTO;
import com.magic.hogwarts.inventory.domain.model.Potion;
import com.magic.hogwarts.inventory.infrastructure.persistence.PotionRepository;
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
class PotionServiceTest {

  /** Repository mock used by the service under test. */
  @Mock private PotionRepository repository;

  /** Service under test. */
  private PotionService service;

  @BeforeEach
  void setUp() {
    service = new PotionService(repository);
  }

  @Test
  void create_mapsFieldsAndReturnsDto() {
    PotionCreateRequestDTO req = new PotionCreateRequestDTO();
    req.setName("Polyjuice Potion");
    req.setEffect("Transform into someone else");
    req.setQuantity(2);

    when(repository.save(any(Potion.class)))
        .thenAnswer(
            inv -> {
              Potion p = inv.getArgument(0);
              p.setId(1L);
              return p;
            });

    PotionDTO dto = service.create(req);

    assertThat(dto.getId()).isEqualTo(1L);
    assertThat(dto.getName()).isEqualTo("Polyjuice Potion");
    assertThat(dto.getEffect()).isEqualTo("Transform into someone else");
    assertThat(dto.getQuantity()).isEqualTo(2);
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

    service.list("poly", null, null, pageable);

    verify(repository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void list_whenAllFiltersProvided_buildsSpecificationAndUsesFindAllSpecification() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

    service.list("poly", "transform", 0, pageable);

    verify(repository).findAll(any(Specification.class), eq(pageable));
  }
}
