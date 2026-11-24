package com.learnflow.note;

import java.time.LocalDateTime;

public class Note {
    private Long id;
    private Long userId;
    private String title;
    private String category;
    private String content;
    private Long revision;
    private String indexStatus;
    private String indexError;
    private Integer indexAttempts;
    private LocalDateTime nextIndexAt;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getRevision() { return revision; }
    public void setRevision(Long revision) { this.revision = revision; }

    public String getIndexStatus() { return indexStatus; }
    public void setIndexStatus(String indexStatus) { this.indexStatus = indexStatus; }

    public String getIndexError() { return indexError; }
    public void setIndexError(String indexError) { this.indexError = indexError; }

    public Integer getIndexAttempts() { return indexAttempts; }
    public void setIndexAttempts(Integer indexAttempts) { this.indexAttempts = indexAttempts; }

    public LocalDateTime getNextIndexAt() { return nextIndexAt; }
    public void setNextIndexAt(LocalDateTime nextIndexAt) { this.nextIndexAt = nextIndexAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
