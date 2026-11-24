package com.learnflow.ai;
import java.time.LocalDateTime;
import com.learnflow.note.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "learnflow.ai.worker-enabled", havingValue = "true", matchIfMissing = true)
public class NoteIndexWorker {
    private final NoteMapper notes;
    private final AiGateway ai;
    public NoteIndexWorker(NoteMapper notes, AiGateway ai) { this.notes = notes; this.ai = ai; }

    @Scheduled(fixedDelay = 3000, initialDelay = 5000)
    public void synchronize() {
        if (!ai.configured()) return;
        for (Note note : notes.pending()) {
            try {
                ai.index(note);
                // 比较 revision：慢请求的旧结果不能覆盖用户刚保存的新版本。
                notes.indexed(note.getId(), note.getRevision());
            } catch (RuntimeException ex) {
                int attempts = note.getIndexAttempts() + 1;
                LocalDateTime next = attempts >= 5 ? null : LocalDateTime.now().plusSeconds(Math.min(300, 5L << attempts));
                notes.failed(note.getId(), note.getRevision(), "索引暂时失败，可稍后手动重试", attempts, next);
            }
        }
    }
}
