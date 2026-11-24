package com.learnflow.task;

import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import java.util.stream.Stream;

import com.learnflow.common.ApiExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TaskUpdateTest {

    @Mock
    private TaskMapper taskMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(new TaskService(taskMapper, mock(ApplicationEventPublisher.class))))
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(TestPrincipal.resolver())
                .build();
    }

    @Test
    void updatesRequestedTaskAndReturnsDatabaseValues() throws Exception {
        Task original = task("Original title", TaskStatus.TODO);
        Task stored = task("Updated title", TaskStatus.IN_PROGRESS);
        stored.setUpdatedAt(stored.getCreatedAt().plusHours(1));
        when(taskMapper.findById(2L, 1L)).thenReturn(original, stored);
        when(taskMapper.update(any(Task.class))).thenReturn(1);

        mockMvc.perform(put("/api/tasks/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  Updated title  \",\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-10T12:00:00"))
                .andExpect(jsonPath("$.updatedAt").value("2026-10-10T13:00:00"));

        ArgumentCaptor<Task> updated = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).update(updated.capture());
        assertEquals(2L, updated.getValue().getId());
        assertEquals("Updated title", updated.getValue().getTitle());
        assertEquals(TaskStatus.IN_PROGRESS, updated.getValue().getStatus());
    }

    @Test
    void repeatingSameUpdateDoesNotTreatExistingTaskAsMissing() throws Exception {
        when(taskMapper.findById(2L, 1L)).thenReturn(task("Updated title", TaskStatus.DONE));
        when(taskMapper.update(any(Task.class))).thenReturn(0);

        mockMvc.perform(put("/api/tasks/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated title\",\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void missingTaskReturns404WithoutUpdating() throws Exception {
        mockMvc.perform(put("/api/tasks/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated title\",\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());

        verify(taskMapper, never()).update(any(Task.class));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void invalidRequestReturns400BeforeAccessingMapper(String body) throws Exception {
        mockMvc.perform(put("/api/tasks/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskMapper);
    }

    static Stream<String> invalidRequests() {
        return Stream.of(
                "{\"title\":\"   \",\"status\":\"TODO\"}",
                "{\"title\":\"" + "x".repeat(201) + "\",\"status\":\"TODO\"}",
                "{\"title\":\"Updated title\"}",
                "{\"title\":\"Updated title\",\"status\":\"UNKNOWN\"}");
    }

    private static Task task(String title, TaskStatus status) {
        Task task = new Task();
        task.setId(2L);
        task.setUserId(1L);
        task.setTitle(title);
        task.setStatus(status);
        task.setCreatedAt(LocalDateTime.of(2026, 10, 10, 12, 0));
        task.setUpdatedAt(task.getCreatedAt());
        return task;
    }
}
