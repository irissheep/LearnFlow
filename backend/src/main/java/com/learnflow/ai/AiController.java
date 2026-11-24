package com.learnflow.ai;
import java.util.Map;
import com.learnflow.note.NoteMapper;
import com.learnflow.user.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    public record Question(@NotBlank @Size(max = 2000) String question) {}
    private final AiGateway ai;
    private final NoteMapper notes;
    public AiController(AiGateway ai, NoteMapper notes) { this.ai = ai; this.notes = notes; }
    @GetMapping("/status")
    public Map<?, ?> status() { return ai.status(); }
    @PostMapping("/ask")
    public AnswerResponse ask(@AuthenticationPrincipal LoginUser user, @Valid @RequestBody Question request) {
        // 传给 Python 的白名单来自实时数据库；已删除或版本过期的向量不能进入模型上下文。
        return ai.ask(user.id(), request.question().strip(), notes.manifest(user.id()));
    }
    @PostMapping("/reindex")
    public Map<String, Integer> reindex(@AuthenticationPrincipal LoginUser user) {
        return Map.of("queued", notes.reindexAll(user.id()));
    }
}
