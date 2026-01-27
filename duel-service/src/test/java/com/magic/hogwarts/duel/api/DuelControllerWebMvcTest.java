package com.magic.hogwarts.duel.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.magic.hogwarts.common.exception.GlobalExceptionHandler;
import com.magic.hogwarts.duel.api.dto.request.CreateDuelRequestDTO;
import com.magic.hogwarts.duel.api.dto.response.DuelDTO;
import com.magic.hogwarts.duel.application.DuelService;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DuelController.class)
@Import(GlobalExceptionHandler.class)
class DuelControllerWebMvcTest {

  /** MVC test client. */
  @Autowired private MockMvc mvc;

  /** Service mock injected into the web slice. */
  @MockitoBean private DuelService service;

  @Test
  void create_whenAttackerIdNotPositive_returnsValidationErrorWithEnglishMessage()
      throws Exception {
    String body =
        """
        {"attackerId":0,"defenderId":1}
        """;

    mvc.perform(post("/api/duels").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Request validation failed"))
        .andExpect(jsonPath("$.path").value("/api/duels"))
        .andExpect(
            jsonPath("$.details.fields.attackerId").value("attackerId must be a positive number"));
  }

  @Test
  void getById_whenMissing_returnsNotFoundApiError() throws Exception {
    when(service.getById(123L)).thenThrow(new NoSuchElementException("Duel not found"));

    mvc.perform(get("/api/duels/123"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Resource not found"))
        .andExpect(jsonPath("$.path").value("/api/duels/123"));
  }

  @Test
  void create_whenServiceThrowsIllegalArgument_returnsBadRequestApiError() throws Exception {
    when(service.create(any(CreateDuelRequestDTO.class)))
        .thenThrow(new IllegalArgumentException("attackerId and defenderId cannot be the same"));

    String body =
        """
        {"attackerId":1,"defenderId":1}
        """;

    mvc.perform(post("/api/duels").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("attackerId and defenderId cannot be the same"))
        .andExpect(jsonPath("$.path").value("/api/duels"));
  }

  @Test
  void create_whenOk_returnsDuelDto() throws Exception {
    DuelDTO dto = new DuelDTO();
    dto.setId(1L);
    dto.setAttackerId(1L);
    dto.setDefenderId(2L);
    dto.setWinnerId(1L);
    dto.setLoserId(2L);
    dto.setMessage("Duel completed");

    when(service.create(any(CreateDuelRequestDTO.class))).thenReturn(dto);

    String body =
        """
        {"attackerId":1,"defenderId":2}
        """;

    mvc.perform(post("/api/duels").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.message").value("Duel completed"))
        .andExpect(jsonPath("$.winnerId").value(1))
        .andExpect(jsonPath("$.loserId").value(2));
  }

  @Test
  void getById_whenOk_returnsDuelDto() throws Exception {
    DuelDTO dto = new DuelDTO();
    dto.setId(123L);
    dto.setMessage("Duel completed");

    when(service.getById(123L)).thenReturn(dto);

    mvc.perform(get("/api/duels/123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(123))
        .andExpect(jsonPath("$.message").value("Duel completed"));
  }
}
