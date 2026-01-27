package com.magic.hogwarts.inventory.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.magic.hogwarts.common.exception.GlobalExceptionHandler;
import com.magic.hogwarts.inventory.api.dto.request.BroomCreateRequestDTO;
import com.magic.hogwarts.inventory.api.dto.response.BroomDTO;
import com.magic.hogwarts.inventory.application.BroomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BroomController.class)
@Import(GlobalExceptionHandler.class)
class BroomControllerWebMvcTest {

  /** MVC test client. */
  @Autowired private MockMvc mvc;

  /** Service mock injected into the web slice. */
  @MockitoBean private BroomService service;

  @Test
  void create_whenOk_returnsBroomDto() throws Exception {
    BroomDTO dto = new BroomDTO();
    dto.setId(1L);
    dto.setModel("Nimbus 2000");
    dto.setSpeedRating(10);
    dto.setQuantity(3);

    when(service.create(any(BroomCreateRequestDTO.class))).thenReturn(dto);

    String body =
        """
        {"model":"Nimbus 2000","speedRating":10,"quantity":3}
        """;

    mvc.perform(post("/api/inventory/brooms").contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.model").value("Nimbus 2000"))
        .andExpect(jsonPath("$.speedRating").value(10))
        .andExpect(jsonPath("$.quantity").value(3));
  }

  @Test
  void list_whenOk_returnsPage() throws Exception {
    when(service.list(any(), any(), any(), any())).thenReturn(Page.empty());

    mvc.perform(get("/api/inventory/brooms"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }
}
