package com.learnflow.task;
import java.net.URI;
import java.util.List;
import com.learnflow.user.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService service;
    public TaskController(TaskService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse task = service.create(user.id(), request);
        return ResponseEntity.created(URI.create("/api/tasks/" + task.id())).body(task);
    }
    @PutMapping("/{id}")
    public TaskResponse update(@AuthenticationPrincipal LoginUser user, @PathVariable long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        return service.update(user.id(), id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal LoginUser user, @PathVariable long id) {
        service.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}")
    public TaskResponse findById(@AuthenticationPrincipal LoginUser user, @PathVariable long id) {
        return service.findById(user.id(), id);
    }
    @GetMapping
    public List<TaskResponse> findAll(@AuthenticationPrincipal LoginUser user) { return service.findAll(user.id()); }
}
