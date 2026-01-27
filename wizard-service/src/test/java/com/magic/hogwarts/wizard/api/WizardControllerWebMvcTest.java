package com.magic.hogwarts.wizard.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.magic.hogwarts.common.exception.GlobalExceptionHandler;
import com.magic.hogwarts.wizard.api.dto.request.WizardPatchRequestDTO;
import com.magic.hogwarts.wizard.api.dto.request.WizardRequestDTO;
import com.magic.hogwarts.wizard.api.dto.response.AttackResultDTO;
import com.magic.hogwarts.wizard.api.dto.response.WizardDTO;
import com.magic.hogwarts.wizard.application.WizardService;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = WizardController.class)
@Import(GlobalExceptionHandler.class)
class WizardControllerWebMvcTest {

  /** MVC test client. */
  @Autowired private MockMvc mvc;

  /** Service mock injected into the web slice. */
  @MockitoBean private WizardService service;

  @Test
  void create_whenNameTooShort_returnsValidationErrorWithEnglishFieldMessage() throws Exception {
    // name length 2 triggers only @Size (not @NotBlank), avoiding duplicate field errors.
    String body =
        """
        {"name":"ab","house":"Gryffindor"}
        """;

    mvc.perform(post("/api/wizards").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Request validation failed"))
        .andExpect(jsonPath("$.path").value("/api/wizards"))
        .andExpect(
            jsonPath("$.details.fields.name")
                .value("Wizard name must be at least 3 characters long"));
  }

  @Test
  void create_whenHouseInvalid_returnsValidationErrorWithEnglishFieldMessage() throws Exception {
    String body =
        """
        {"name":"Harry Potter","house":"Durmstrang"}
        """;

    mvc.perform(post("/api/wizards").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Request validation failed"))
        .andExpect(jsonPath("$.path").value("/api/wizards"))
        .andExpect(
            jsonPath("$.details.fields.house")
                .value(
                    "House must be one of Hogwarts' 4 official houses (or blank so the Sorting Hat can decide)"));
  }

  @Test
  void getById_whenServiceThrowsNoSuchElement_returnsNotFoundApiError() throws Exception {
    when(service.getById(1L)).thenThrow(new NoSuchElementException("Wizard not found"));

    mvc.perform(get("/api/wizards/1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Resource not found"))
        .andExpect(jsonPath("$.path").value("/api/wizards/1"));
  }

  @Test
  void create_whenServiceThrowsIllegalArgument_returnsBadRequestApiError() throws Exception {
    when(service.createWizard(any(WizardRequestDTO.class)))
        .thenThrow(new IllegalArgumentException("boom"));

    String body =
        """
        {"name":"Harry Potter","house":"Gryffindor"}
        """;

    mvc.perform(post("/api/wizards").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("boom"))
        .andExpect(jsonPath("$.path").value("/api/wizards"));
  }

  @Test
  void patch_whenServiceThrowsIllegalState_returnsConflictApiError() throws Exception {
    when(service.patch(eq(5L), any(WizardPatchRequestDTO.class)))
        .thenThrow(new IllegalStateException("conflict"));

    String body =
        """
        {"name":"Hermione Granger"}
        """;

    mvc.perform(patch("/api/wizards/5").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"))
        .andExpect(jsonPath("$.message").value("conflict"))
        .andExpect(jsonPath("$.path").value("/api/wizards/5"));
  }

  @Test
  void delete_whenOk_returns200Or204() throws Exception {
    mvc.perform(delete("/api/wizards/9")).andExpect(status().isOk());
  }

  @Test
  void create_whenOk_returnsWizardDto() throws Exception {
    WizardDTO dto = new WizardDTO();
    dto.setId(1L);
    dto.setName("Harry Potter");
    dto.setHouse("Gryffindor");
    dto.setLevel(1);
    dto.setWandDescription("No wand assigned");

    when(service.createWizard(any(WizardRequestDTO.class))).thenReturn(dto);

    String body =
        """
        {"name":"Harry Potter","house":"Gryffindor"}
        """;

    mvc.perform(post("/api/wizards").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Harry Potter"))
        .andExpect(jsonPath("$.house").value("Gryffindor"))
        .andExpect(jsonPath("$.wandDescription").value("No wand assigned"));
  }

  @Test
  void getAll_whenOk_returnsPage() throws Exception {
    when(service.list(any(), any(), any())).thenReturn(Page.empty());

    mvc.perform(get("/api/wizards"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void getById_whenOk_returnsWizardDto() throws Exception {
    WizardDTO dto = new WizardDTO();
    dto.setId(1L);
    dto.setName("Harry Potter");
    dto.setHouse("Gryffindor");
    dto.setLevel(1);
    dto.setWandDescription("No wand assigned");

    when(service.getById(1L)).thenReturn(dto);

    mvc.perform(get("/api/wizards/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Harry Potter"));
  }

  @Test
  void patch_whenOk_returnsWizardDto() throws Exception {
    WizardDTO dto = new WizardDTO();
    dto.setId(5L);
    dto.setName("Hermione Granger");
    dto.setHouse("Gryffindor");

    when(service.patch(eq(5L), any(WizardPatchRequestDTO.class))).thenReturn(dto);

    String body =
        """
        {"name":"Hermione Granger"}
        """;

    mvc.perform(patch("/api/wizards/5").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.name").value("Hermione Granger"));
  }

  @Test
  void attack_whenOk_returnsAttackResult() throws Exception {
    AttackResultDTO result = new AttackResultDTO();
    result.setAttackerId(1L);
    result.setDefenderId(2L);
    result.setWinnerId(1L);
    result.setLoserId(2L);
    result.setMessage("Duel completed");

    when(service.attack(1L, 2L)).thenReturn(result);

    String body =
        """
        {"attackerId":1,"defenderId":2}
        """;

    mvc.perform(post("/api/wizards/attack").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.winnerId").value(1))
        .andExpect(jsonPath("$.message").value("Duel completed"));
  }
}
