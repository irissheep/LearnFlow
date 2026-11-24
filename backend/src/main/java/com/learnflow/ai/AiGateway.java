package com.learnflow.ai;
import java.time.Duration;
import java.util.*;
import com.learnflow.common.ApiException;
import com.learnflow.note.Note;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class AiGateway {
    private final RestClient client;
    private final boolean configured;
    public AiGateway(@Value("${learnflow.ai.url}") String url, @Value("${learnflow.ai.token:}") String token) {
        this.configured = !token.isBlank();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(35));
        client = RestClient.builder().baseUrl(url).requestFactory(factory)
                .defaultHeader("X-Internal-Token", token).build();
    }
    public boolean configured() { return configured; }
    public Map<?, ?> status() {
        if (!configured) return Map.of("available", false, "mode", "unconfigured");
        try {
            Map<?, ?> health = client.get().uri("/health").retrieve().body(Map.class);
            return Map.of("available", true, "mode", health == null ? "unknown" : health.get("mode"));
        } catch (RestClientException ex) { return Map.of("available", false, "mode", "offline"); }
    }
    public void index(Note note) {
        requireConfigured();
        String endpoint = note.getDeletedAt() == null ? "/internal/index" : "/internal/delete";
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", note.getUserId()); payload.put("note_id", note.getId()); payload.put("revision", note.getRevision());
        if (note.getDeletedAt() == null) { payload.put("title", note.getTitle()); payload.put("content", note.getContent()); }
        try { client.post().uri(endpoint).body(payload).retrieve().toBodilessEntity(); }
        catch (RestClientException ex) { throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI 索引服务暂时不可用"); }
    }
    public AnswerResponse ask(long userId, String question, List<NoteManifest> notes) {
        requireConfigured();
        try {
            AnswerResponse answer = client.post().uri("/internal/query")
                    .body(Map.of("user_id", userId, "question", question, "notes", notes))
                    .retrieve().body(AnswerResponse.class);
            if (answer == null) throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 服务返回空结果");
            return answer;
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI 服务超时或不可用，请稍后重试");
        }
    }
    private void requireConfigured() {
        if (!configured) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "尚未配置 AI 服务，请按 README 设置服务令牌");
    }
}
