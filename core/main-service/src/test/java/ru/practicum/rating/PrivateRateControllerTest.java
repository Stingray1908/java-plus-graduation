package ru.practicum.rating;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.error.exception.ConflictException;
import ru.practicum.error.exception.NotFoundException;
import ru.practicum.rating.controller.PrivateRateController;
import ru.practicum.rating.service.RateService;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateRateController.class)
class PrivateRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RateService rateService;

    @Test
    void addRate_shouldReturn201() throws Exception {
        mockMvc.perform(post("/users/1/events/2/rate")
                        .param("isLike", "true"))
                .andExpect(status().isCreated());

        verify(rateService).addRate(1L, 2L, true);
    }

    @Test
    void deleteRate_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/users/1/events/2/rate"))
                .andExpect(status().isNoContent());

        verify(rateService).deleteRate(1L, 2L);
    }

    @Test
    void addRate_whenEventNotFound_shouldReturn404() throws Exception {
        doThrow(new NotFoundException("Not found")).when(rateService).addRate(1L, 999L, true);

        mockMvc.perform(post("/users/1/events/999/rate")
                        .param("isLike", "true"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addRate_whenInitiatorRatesOwnEvent_shouldReturn409() throws Exception {
        doThrow(new ConflictException("Conflict")).when(rateService).addRate(1L, 2L, true);

        mockMvc.perform(post("/users/1/events/2/rate")
                        .param("isLike", "true"))
                .andExpect(status().isConflict());
    }
}