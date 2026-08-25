package ru.practicum.comments;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.comments.controller.PrivateCommentController;
import ru.practicum.comments.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateCommentController.class)
class PrivateCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    private final CommentDto commentDto = CommentDto.builder()
            .id(1L)
            .text("Great event!")
            .eventId(100L)
            .authorId(10L)
            .authorName("John Doe")
            .status(CommentStatus.PENDING)
            .createdOn(LocalDateTime.now().minusHours(1))
            .updatedOn(null)
            .build();

    @Test
    void addComment_shouldReturnCreatedComment() throws Exception {
        NewCommentDto request = new NewCommentDto();
        request.setText("Amazing lecture!");

        when(commentService.addComment(eq(10L), eq(100L), any(NewCommentDto.class)))
                .thenReturn(commentDto);

        mockMvc.perform(post("/users/10/events/100/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Great event!"))
                .andExpect(jsonPath("$.authorName").value("John Doe"));

        verify(commentService).addComment(10L, 100L, request);
    }

    @Test
    void addComment_textEmpty_shouldReturn400() throws Exception {
        NewCommentDto request = new NewCommentDto();
        request.setText("");

        mockMvc.perform(post("/users/10/events/100/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addComment_textNull_shouldReturn400() throws Exception {
        NewCommentDto request = new NewCommentDto();
        request.setText(null);

        mockMvc.perform(post("/users/10/events/100/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getComment_shouldReturnComment() throws Exception {
        when(commentService.getCommentById(1L)).thenReturn(commentDto);

        mockMvc.perform(get("/users/10/events/100/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.text").value("Great event!"));
    }

    @Test
    void getComment_notFound_shouldReturn404() throws Exception {
        when(commentService.getCommentById(999L))
                .thenThrow(new ru.practicum.error.exception.NotFoundException("Not found"));

        mockMvc.perform(get("/users/10/events/100/comments/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getComments_shouldReturnListOfComments() throws Exception {
        when(commentService.getCommentsByEventId(100L, 0, 10))
                .thenReturn(List.of(commentDto));

        mockMvc.perform(get("/users/10/events/100/comments")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].text").value("Great event!"));
    }

    @Test
    void updateComment_byAuthor_shouldReturnUpdated() throws Exception {
        UpdateCommentByModeratorRequest request = new UpdateCommentByModeratorRequest();
        request.setText("Updated text");

        CommentDto updated = CommentDto.builder()
                .text("Updated text")
                .updatedOn(LocalDateTime.now())
                .build();

        when(commentService.updateCommentByAuthor(eq(10L), eq(1L), any()))
                .thenReturn(updated);

        mockMvc.perform(patch("/users/10/events/100/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated text"));
    }

    @Test
    void updateComment_invalidTextLength_shouldReturn400() throws Exception {
        UpdateCommentByModeratorRequest request = new UpdateCommentByModeratorRequest();
        request.setText("   ");

        mockMvc.perform(patch("/users/10/events/100/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteComment_byAuthor_shouldReturn204() throws Exception {
        doNothing().when(commentService).deleteCommentByAuthor(10L, 1L);

        mockMvc.perform(delete("/users/10/events/100/comments/1"))
                .andExpect(status().isNoContent());

        verify(commentService).deleteCommentByAuthor(10L, 1L);
    }

    @Test
    void deleteComment_notOwner_shouldFail_inService() throws Exception {
        doThrow(new IllegalArgumentException("You can only delete your own comments"))
                .when(commentService).deleteCommentByAuthor(15L, 1L);

        mockMvc.perform(delete("/users/15/events/100/comments/1"))
                .andExpect(status().isBadRequest());
    }
}