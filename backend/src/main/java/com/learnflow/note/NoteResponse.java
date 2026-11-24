package com.learnflow.note;
import java.time.LocalDateTime;
public record NoteResponse(Long id, String title, String category, String content, long revision,
        String indexStatus, String indexError, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getTitle(), note.getCategory(), note.getContent(),
                note.getRevision(), note.getIndexStatus(), note.getIndexError(), note.getCreatedAt(), note.getUpdatedAt());
    }
}
