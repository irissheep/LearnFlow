package com.learnflow.task;
import java.util.List;
import com.learnflow.stats.TaskChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private final TaskMapper mapper;
    private final ApplicationEventPublisher events;
    public TaskService(TaskMapper mapper, ApplicationEventPublisher events) {
        this.mapper = mapper; this.events = events;
    }
    @Transactional
    public TaskResponse create(long userId, CreateTaskRequest request) {
        Task task = new Task();
        task.setUserId(userId);
        task.setTitle(request.title().strip());
        task.setStatus(TaskStatus.TODO);
        mapper.insert(task);
        events.publishEvent(new TaskChanged(userId));
        return TaskResponse.from(mapper.findById(task.getId(), userId));
    }
    @Transactional
    public TaskResponse update(long userId, long id, UpdateTaskRequest request) {
        Task task = requireTask(userId, id);
        task.setTitle(request.title().strip());
        task.setStatus(request.status());
        mapper.update(task);
        events.publishEvent(new TaskChanged(userId));
        return TaskResponse.from(mapper.findById(id, userId));
    }
    @Transactional
    public void delete(long userId, long id) {
        if (mapper.delete(id, userId) == 0) throw new TaskNotFoundException(id);
        events.publishEvent(new TaskChanged(userId));
    }
    public TaskResponse findById(long userId, long id) { return TaskResponse.from(requireTask(userId, id)); }
    public List<TaskResponse> findAll(long userId) {
        return mapper.findAll(userId).stream().map(TaskResponse::from).toList();
    }
    private Task requireTask(long userId, long id) {
        Task task = mapper.findById(id, userId);
        // 对“不存在”和“不属于当前用户”统一返回 404，不泄露其他用户的数据存在性。
        if (task == null) throw new TaskNotFoundException(id);
        return task;
    }
}
