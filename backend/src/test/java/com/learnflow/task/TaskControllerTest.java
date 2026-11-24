package com.learnflow.task;
import com.learnflow.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.profiles.active=test",
    "spring.datasource.url=jdbc:h2:mem:core-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "learnflow.cache-enabled=false", "learnflow.ai.worker-enabled=false", "learnflow.ai.token="
})
class TaskControllerTest {
    @Value("${local.server.port}") int port;

    @Test
    void authenticationOwnershipCrudAndStatsWorkTogether() throws Exception {
        ApiClient alice = new ApiClient(port);
        ApiClient bob = new ApiClient(port);
        assertEquals(401, alice.get("/api/tasks").status());
        assertEquals(403, alice.request("POST", "/api/tasks", "{\"title\":\"No CSRF\"}", false).status());
        alice.registerAndLogin("alice_core");
        bob.registerAndLogin("bob_core");
        assertEquals(409, alice.post("/api/auth/register",
                "{\"username\":\"alice_core\",\"password\":\"test-password-123\",\"nickname\":\"Other\"}").status());
        assertEquals("alice_core", alice.get("/api/auth/me").text("$.username"));

        ApiClient.Response created = alice.post("/api/tasks", "{\"title\":\"  Learn Spring  \"}");
        assertEquals(201, created.status());
        long taskId = created.number("$.id");
        assertEquals("Learn Spring", created.text("$.title"));
        assertEquals("TODO", created.text("$.status"));
        assertEquals(404, bob.get("/api/tasks/" + taskId).status());
        assertEquals(404, bob.put("/api/tasks/" + taskId, "{\"title\":\"stolen\",\"status\":\"DONE\"}").status());
        assertEquals(404, bob.delete("/api/tasks/" + taskId).status());
        assertEquals(400, alice.put("/api/tasks/" + taskId, "{\"title\":\"x\",\"status\":\"WRONG\"}").status());
        assertEquals(400, alice.post("/api/tasks", "{\"title\":\"   \"}").status());
        assertEquals(400, alice.post("/api/tasks", "{\"title\":\"" + "x".repeat(201) + "\"}").status());
        assertEquals(200, alice.put("/api/tasks/" + taskId, "{\"title\":\"Learn Spring\",\"status\":\"DONE\"}").status());
        assertEquals(200, alice.put("/api/tasks/" + taskId, "{\"title\":\"Learn Spring\",\"status\":\"DONE\"}").status());

        ApiClient.Response note = alice.post("/api/notes",
                "{\"title\":\"Transactions\",\"category\":\"Java\",\"content\":\"Spring transaction notes\"}");
        assertEquals(201, note.status());
        long noteId = note.number("$.id");
        assertEquals("PENDING", note.text("$.indexStatus"));
        assertEquals(404, bob.get("/api/notes/" + noteId).status());
        assertEquals(404, bob.put("/api/notes/" + noteId, "{\"title\":\"stolen\",\"content\":\"stolen\"}").status());
        assertEquals(404, bob.delete("/api/notes/" + noteId).status());
        assertEquals(404, bob.post("/api/notes/" + noteId + "/retry-index", "{}").status());
        assertEquals(0, bob.get("/api/notes").number("$.total"));
        assertEquals(1, alice.get("/api/notes?keyword=transaction&category=Java&size=1").number("$.total"));
        assertEquals(400, alice.get("/api/notes?page=-1").status());
        assertEquals(200, alice.put("/api/notes/" + noteId,
                "{\"title\":\"Updated\",\"category\":\"Java\",\"content\":\"Updated transaction notes\"}").status());
        assertEquals(2, alice.get("/api/notes/" + noteId).number("$.revision"));

        ApiClient.Response stats = alice.get("/api/stats");
        assertEquals(200, stats.status());
        assertEquals(1, stats.number("$.totalTasks"));
        assertEquals(1, stats.number("$.completedTasks"));
        assertEquals(1, stats.number("$.totalNotes"));
        assertEquals(0, bob.get("/api/stats").number("$.totalTasks"));
        assertEquals(503, alice.post("/api/ai/ask", "{\"question\":\"Spring?\"}").status());
        assertEquals(204, alice.delete("/api/tasks/" + taskId).status());
        assertEquals(404, alice.get("/api/tasks/" + taskId).status());
        assertEquals(204, alice.delete("/api/notes/" + noteId).status());
        assertEquals(0, alice.get("/api/notes").number("$.total"));
        assertEquals(204, alice.post("/api/auth/logout", "{}").status());
        assertEquals(401, alice.get("/api/auth/me").status());
        assertEquals(401, alice.post("/api/auth/login",
                "{\"username\":\"alice_core\",\"password\":\"wrong-password\"}").status());
    }
}
