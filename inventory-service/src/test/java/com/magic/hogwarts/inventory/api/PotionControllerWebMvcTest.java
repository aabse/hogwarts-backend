package com.magic.hogwarts.inventory.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.magic.hogwarts.common.exception.GlobalExceptionHandler;
import com.magic.hogwarts.inventory.api.dto.request.PotionCreateRequestDTO;
import com.magic.hogwarts.inventory.api.dto.response.PotionDTO;
import com.magic.hogwarts.inventory.application.PotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PotionController.class)
@Import(GlobalExceptionHandler.class)
class PotionControllerWebMvcTest {

  /** MVC test client. */
  @Autowired private MockMvc mvc;

  /** Service mock injected into the web slice. */
  @MockitoBean private PotionService service;

  @Test
  void create_whenOk_returnsPotionDto() throws Exception {
    PotionDTO dto = new PotionDTO();
    dto.setId(1L);
    dto.setName("Polyjuice Potion");
    dto.setEffect("Transform into someone else");
    dto.setQuantity(2);

    when(service.create(any(PotionCreateRequestDTO.class))).thenReturn(dto);

    String body =
        """
        {"name":"Polyjuice Potion","effect":"Transform into someone else","quantity":2}
        """;

    mvc.perform(post("/api/inventory/potions").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Polyjuice Potion"))
        .andExpect(jsonPath("$.effect").value("Transform into someone else"))
        .andExpect(jsonPath("$.quantity").value(2));
  }

  @Test
  void list_whenOk_returnsPage() throws Exception {
    when(service.list(any(), any(), any(), any())).thenReturn(Page.empty());

    mvc.perform(get("/api/inventory/potions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }
}
