package com.learnflow;
import com.learnflow.ai.*;
import com.learnflow.note.NoteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "LEARNFLOW_AI_TEST_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.profiles.active=test",
    "spring.datasource.url=jdbc:h2:mem:ai-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "learnflow.cache-enabled=false", "learnflow.ai.worker-enabled=false",
    "learnflow.ai.token=test-internal-token-for-learnflow-123456"
})
class AiBridgeTest {
    @Value("${local.server.port}") int port;
    @Autowired NoteMapper notes;
    @Autowired AiGateway gateway;
    @DynamicPropertySource
    static void aiUrl(DynamicPropertyRegistry properties) {
        properties.add("learnflow.ai.url", () -> System.getenv("LEARNFLOW_AI_TEST_URL"));
    }

    @Test
    void javaCallsRealPythonAndExcludesDeletedAndOutdatedNotes() throws Exception {
        ApiClient alice = new ApiClient(port);
        ApiClient bob = new ApiClient(port);
        alice.registerAndLogin("alice_ai");
        bob.registerAndLogin("bob_ai");
        ApiClient.Response note = alice.post("/api/notes",
                "{\"title\":\"Transactions\",\"category\":\"Java\",\"content\":\"Spring transaction rollback happens for runtime exceptions.\"}");
        long id = note.number("$.id");
        NoteIndexWorker worker = new NoteIndexWorker(notes, gateway);
        worker.synchronize();
        assertEquals("READY", alice.get("/api/notes/" + id).text("$.indexStatus"));
        String question = "{\"question\":\"Spring transaction rollback\"}";
        ApiClient.Response answer = alice.post("/api/ai/ask", question);
        assertEquals(200, answer.status());
        assertEquals("local", answer.text("$.mode"));
        assertEquals(id, answer.number("$.sources[0].noteId"));
        assertEquals(0, ((java.util.List<?>) bob.post("/api/ai/ask", question).json("$.sources")).size());

        alice.put("/api/notes/" + id,
                "{\"title\":\"Updated transactions\",\"category\":\"Java\",\"content\":\"Spring transaction rollback updated explanation.\"}");
        assertEquals(0, ((java.util.List<?>) alice.post("/api/ai/ask", question).json("$.sources")).size());
        worker.synchronize();
        assertEquals("Updated transactions", alice.post("/api/ai/ask", question).text("$.sources[0].title"));
        alice.delete("/api/notes/" + id);
        assertEquals(0, ((java.util.List<?>) alice.post("/api/ai/ask", question).json("$.sources")).size());
        worker.synchronize();
    }
}
