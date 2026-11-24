package com.learnflow.stats;
import java.util.List;
import java.time.LocalDateTime;
public record StatsResponse(long totalTasks, long completedTasks, long inProgressTasks, long todoTasks,
        double completionRate, long totalNotes, List<RecentNote> recentNotes) {
    public record RecentNote(long id, String title, String category, LocalDateTime updatedAt) {}
}
