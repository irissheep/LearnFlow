package com.learnflow.stats;
import com.learnflow.note.NoteMapper;
import com.learnflow.task.TaskMapper;
import com.learnflow.user.LoginUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final TaskMapper tasks;
    private final NoteMapper notes;
    private final StatsCache cache;
    public StatsController(TaskMapper tasks, NoteMapper notes, StatsCache cache) {
        this.tasks = tasks; this.notes = notes; this.cache = cache;
    }
    @GetMapping
    public StatsResponse get(@AuthenticationPrincipal LoginUser user) {
        TaskCounts counts = cache.get(user.id());
        if (counts == null) { counts = tasks.counts(user.id()); cache.put(user.id(), counts); }
        double rate = counts.total() == 0 ? 0 : Math.round(counts.done() * 1000.0 / counts.total()) / 10.0;
        // 最近笔记不缓存，避免为了缓存而增加无用的失效依赖。
        return new StatsResponse(counts.total(), counts.done(), counts.inProgress(),
                counts.total() - counts.done() - counts.inProgress(), rate, notes.count(user.id(), null, null),
                notes.findPage(user.id(), null, null, 0, 5).stream()
                    .map(note -> new StatsResponse.RecentNote(note.getId(), note.getTitle(), note.getCategory(), note.getUpdatedAt())).toList());
    }
}
