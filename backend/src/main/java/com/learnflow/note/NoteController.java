package com.learnflow.note;
import java.net.URI;
import java.util.List;
import com.learnflow.common.PageResponse;
import com.learnflow.user.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService service;
    public NoteController(NoteService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<NoteResponse> create(@AuthenticationPrincipal LoginUser user, @Valid @RequestBody NoteRequest request) {
        NoteResponse note = service.create(user.id(), request);
        return ResponseEntity.created(URI.create("/api/notes/" + note.id())).body(note);
    }
    @GetMapping
    public PageResponse<NoteResponse> list(@AuthenticationPrincipal LoginUser user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String category) {
        return service.list(user.id(), page, size, keyword, category);
    }
    @GetMapping("/categories")
    public List<String> categories(@AuthenticationPrincipal LoginUser user) { return service.categories(user.id()); }
    @GetMapping("/{id}")
    public NoteResponse get(@AuthenticationPrincipal LoginUser user, @PathVariable long id) { return service.get(user.id(), id); }
    @PutMapping("/{id}")
    public NoteResponse update(@AuthenticationPrincipal LoginUser user, @PathVariable long id, @Valid @RequestBody NoteRequest request) {
        return service.update(user.id(), id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal LoginUser user, @PathVariable long id) {
        service.delete(user.id(), id); return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/retry-index")
    public NoteResponse retry(@AuthenticationPrincipal LoginUser user, @PathVariable long id) { return service.retry(user.id(), id); }
}
