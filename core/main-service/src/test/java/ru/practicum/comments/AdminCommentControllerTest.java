package ru.practicum.comments;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.comments.controller.AdminCommentController;
import ru.practicum.comments.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCommentController.class)
class AdminCommentControllerTest {

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
    void getAllComments_shouldReturnListOfComments() throws Exception {
        when(commentService.getCommentsByEventId(any(), anyInt(), anyInt()))
                .thenReturn(List.of(commentDto));

        mockMvc.perform(get("/admin/comments")
                        .param("eventId", "100")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].text").value("Great event!"))
                .andExpect(jsonPath("$[0].authorName").value("John Doe"));

        verify(commentService).getCommentsByEventId(eq(100L), eq(0), eq(10));
    }

    @Test
    void getAllComments_noParams_shouldReturnAllWithDefaultPagination() throws Exception {
        when(commentService.getCommentsByEventId(isNull(), anyInt(), anyInt()))
                .thenReturn(List.of(commentDto));

        mockMvc.perform(get("/admin/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(commentService).getCommentsByEventId(isNull(), eq(0), eq(10));
    }

    @Test
    void moderateComment_shouldApproveComment() throws Exception {
        UpdateCommentByModeratorRequest request = new UpdateCommentByModeratorRequest();
        request.setStatus(CommentStatus.APPROVED);
        request.setText("Updated text");

        CommentDto updatedDto = CommentDto.builder()
                .status(CommentStatus.APPROVED)
                .text("Updated text")
                .updatedOn(LocalDateTime.now())
                .build();

        when(commentService.updateCommentByModerator(any(), anyLong(), any()))
                .thenReturn(updatedDto);

        mockMvc.perform(patch("/admin/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.text").value("Updated text"));

        verify(commentService).updateCommentByModerator(null, 1L, request);
    }

    @Test
    void moderateComment_rejectComment_shouldReturnUpdatedStatus() throws Exception {
        UpdateCommentByModeratorRequest request = new UpdateCommentByModeratorRequest();
        request.setStatus(CommentStatus.REJECTED);

        CommentDto rejectedDto = CommentDto.builder()
                .status(CommentStatus.REJECTED)
                .updatedOn(LocalDateTime.now())
                .build();

        when(commentService.updateCommentByModerator(any(), anyLong(), any()))
                .thenReturn(rejectedDto);

        mockMvc.perform(patch("/admin/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void deleteCommentByAdmin_shouldReturn204() throws Exception {
        doNothing().when(commentService).deleteCommentByModerator(any(), anyLong());

        mockMvc.perform(delete("/admin/comments/1"))
                .andExpect(status().isNoContent());

        verify(commentService).deleteCommentByModerator(null, 1L);
    }

    @Test
    void moderateComment_invalidStatus_shouldReturn400() throws Exception {
        UpdateCommentByModeratorRequest request = new UpdateCommentByModeratorRequest();
        request.setStatus(null);

        mockMvc.perform(patch("/admin/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}