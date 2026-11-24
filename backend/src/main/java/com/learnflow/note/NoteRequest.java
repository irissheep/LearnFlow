package com.learnflow.note;
import jakarta.validation.constraints.*;
public record NoteRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 50) String category,
        @NotBlank @Size(max = 100000) String content) {}
