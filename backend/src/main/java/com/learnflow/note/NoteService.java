package com.learnflow.note;
import java.util.List;
import com.learnflow.common.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {
    private final NoteMapper mapper;
    public NoteService(NoteMapper mapper) { this.mapper = mapper; }

    @Transactional
    public NoteResponse create(long userId, NoteRequest request) {
        Note note = new Note(); note.setUserId(userId); apply(note, request);
        mapper.insert(note);
        return get(userId, note.getId());
    }
    @Transactional
    public NoteResponse update(long userId, long id, NoteRequest request) {
        Note note = require(userId, id); apply(note, request); mapper.update(note);
        return get(userId, id);
    }
    @Transactional
    public void delete(long userId, long id) {
        if (mapper.delete(id, userId) == 0) throw missing();
    }
    @Transactional
    public NoteResponse retry(long userId, long id) {
        if (mapper.retry(id, userId) == 0) throw missing();
        return get(userId, id);
    }
    public NoteResponse get(long userId, long id) { return NoteResponse.from(require(userId, id)); }
    public PageResponse<NoteResponse> list(long userId, int page, int size, String keyword, String category) {
        if (page < 0 || page > 100000 || size < 1 || size > 100)
            throw new ApiException(HttpStatus.BAD_REQUEST, "分页参数超出范围");
        if (keyword != null && keyword.length() > 200 || category != null && category.length() > 50)
            throw new ApiException(HttpStatus.BAD_REQUEST, "搜索条件过长");
        return new PageResponse<>(mapper.findPage(userId, keyword, category, page * size, size)
                .stream().map(NoteResponse::from).toList(), page, size, mapper.count(userId, keyword, category));
    }
    public List<String> categories(long userId) { return mapper.categories(userId); }
    private Note require(long userId, long id) {
        Note note = mapper.findById(id, userId);
        if (note == null) throw missing();
        return note;
    }
    private ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND, "笔记不存在"); }
    private void apply(Note note, NoteRequest request) {
        note.setTitle(request.title().strip());
        note.setContent(request.content());
        note.setCategory(request.category() == null || request.category().isBlank() ? "未分类" : request.category().strip());
    }
}
