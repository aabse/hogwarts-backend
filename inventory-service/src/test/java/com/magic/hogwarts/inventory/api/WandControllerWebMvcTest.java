package com.magic.hogwarts.inventory.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.magic.hogwarts.common.exception.GlobalExceptionHandler;
import com.magic.hogwarts.inventory.api.dto.request.AssignWandRequestDTO;
import com.magic.hogwarts.inventory.api.dto.request.WandCreateRequestDTO;
import com.magic.hogwarts.inventory.api.dto.response.WandDTO;
import com.magic.hogwarts.inventory.application.WandService;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = WandController.class)
@Import(GlobalExceptionHandler.class)
class WandControllerWebMvcTest {

  /** MVC test client. */
  @Autowired private MockMvc mvc;

  /** Service mock injected into the web slice. */
  @MockitoBean private WandService service;

  @Test
  void assign_whenWizardIdNotPositive_returnsValidationErrorWithEnglishFieldMessage()
      throws Exception {
    String body =
        """
        {"wizardId":0}
        """;

    mvc.perform(post("/api/inventory/wands/1/assign").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Request validation failed"))
        .andExpect(jsonPath("$.path").value("/api/inventory/wands/1/assign"))
        .andExpect(
            jsonPath("$.details.fields.wizardId").value("wizardId must be a positive number"));
  }

  @Test
  void assign_whenServiceThrowsNoSuchElement_returnsNotFoundApiError() throws Exception {
    when(service.assign(eq(1L), any(AssignWandRequestDTO.class)))
        .thenThrow(new NoSuchElementException("Wand not found"));

    String body =
        """
        {"wizardId":10}
        """;

    mvc.perform(post("/api/inventory/wands/1/assign").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Resource not found"))
        .andExpect(jsonPath("$.path").value("/api/inventory/wands/1/assign"));
  }

  @Test
  void assign_whenServiceThrowsIllegalState_returnsConflictApiError() throws Exception {
    when(service.assign(eq(1L), any(AssignWandRequestDTO.class)))
        .thenThrow(new IllegalStateException("The wand is already assigned to another wizard"));

    String body =
        """
        {"wizardId":10}
        """;

    mvc.perform(post("/api/inventory/wands/1/assign").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"))
        .andExpect(jsonPath("$.message").value("The wand is already assigned to another wizard"))
        .andExpect(jsonPath("$.path").value("/api/inventory/wands/1/assign"));
  }

  @Test
  void assign_whenOk_returnsWandDto() throws Exception {
    WandDTO dto = new WandDTO();
    dto.setId(1L);
    dto.setAssignedWizardId(10L);

    when(service.assign(eq(1L), any(AssignWandRequestDTO.class))).thenReturn(dto);

    String body =
        """
        {"wizardId":10}
        """;

    mvc.perform(post("/api/inventory/wands/1/assign").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.assignedWizardId").value(10));
  }

  @Test
  void create_whenOk_returnsWandDto() throws Exception {
    WandDTO dto = new WandDTO();
    dto.setId(1L);
    dto.setWood("Holly");
    dto.setCore("Phoenix feather");
    dto.setLength(11.0);
    dto.setDescription("Highly compatible with its wizard");

    when(service.create(any(WandCreateRequestDTO.class))).thenReturn(dto);

    String body =
        """
        {"wood":"Holly","core":"Phoenix feather","length":11.0,"description":"Highly compatible with its wizard"}
        """;

    mvc.perform(post("/api/inventory/wands").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.wood").value("Holly"))
        .andExpect(jsonPath("$.core").value("Phoenix feather"))
        .andExpect(jsonPath("$.length").value(11.0));
  }

  @Test
  void list_whenOk_returnsPage() throws Exception {
    when(service.list(any(), any(), any(), any(), any())).thenReturn(Page.empty());

    mvc.perform(get("/api/inventory/wands"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }
}
