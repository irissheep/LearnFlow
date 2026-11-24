package com.learnflow.task;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        TaskStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getStatus(),
                task.getCreatedAt(), task.getUpdatedAt());
    }
}
