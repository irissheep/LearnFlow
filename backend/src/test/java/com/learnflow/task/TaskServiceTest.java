package com.learnflow.task;

import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createsTaskWithTrimmedTitleAndTodoStatus() {
        Task storedTask = new Task();
        storedTask.setId(7L);
        storedTask.setTitle("学习 Spring Boot");
        storedTask.setStatus(TaskStatus.TODO);
        storedTask.setCreatedAt(LocalDateTime.of(2026, 10, 10, 12, 0));
        storedTask.setUpdatedAt(storedTask.getCreatedAt());

        doAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(7L);
            return 1;
        }).when(taskMapper).insert(any(Task.class));
        when(taskMapper.findById(7L, 1L)).thenReturn(storedTask);

        TaskResponse response = taskService.create(1L, new CreateTaskRequest("  学习 Spring Boot  "));

        ArgumentCaptor<Task> insertedTask = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).insert(insertedTask.capture());
        assertEquals("学习 Spring Boot", insertedTask.getValue().getTitle());
        assertEquals(1L, insertedTask.getValue().getUserId());
        assertEquals(TaskStatus.TODO, insertedTask.getValue().getStatus());
        assertEquals(7L, response.id());
        assertEquals(storedTask.getCreatedAt(), response.createdAt());
    }

    @Test
    void rejectsQueryForMissingTask() {
        when(taskMapper.findById(999L, 1L)).thenReturn(null);

        assertThrows(TaskNotFoundException.class, () -> taskService.findById(1L, 999L));
    }
}
