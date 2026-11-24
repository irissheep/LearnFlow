package com.learnflow;
import java.net.ServerSocket;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import com.learnflow.ApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "LEARNFLOW_REDIS_BINARY", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.profiles.active=test",
    "spring.datasource.url=jdbc:h2:mem:cache-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "learnflow.cache-enabled=true", "learnflow.ai.worker-enabled=false", "learnflow.ai.token=",
    "spring.data.redis.password="
})
class CacheBridgeTest {
    private static final int redisPort = freePort();
    @Value("${local.server.port}") int port;
    @Autowired StringRedisTemplate redis;

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) { return socket.getLocalPort(); }
        catch (Exception ex) { throw new RuntimeException(ex); }
    }
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.data.redis.port", () -> redisPort);
    }
    @Test
    void realRedisUsesTtlInvalidatesAfterCommitAndFailsOpen() throws Exception {
        Path directory = Files.createTempDirectory(Path.of(System.getProperty("java.io.tmpdir")), "learnflow-redis-");
        Path config = directory.resolve("redis.conf");
        Files.writeString(config, "bind 127.0.0.1\nport " + redisPort +
                "\nsave \"\"\nappendonly no\ndir .\n");
        Process process = new ProcessBuilder(System.getenv("LEARNFLOW_REDIS_BINARY"), "redis.conf")
                .directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(directory.resolve("redis.log").toFile()).start();
        try {
            boolean ready = false;
            for (int i = 0; i < 30; i++) {
                try {
                    redis.opsForValue().set("learnflow:probe", "1", Duration.ofSeconds(5));
                    ready = true; break;
                } catch (RuntimeException ex) { Thread.sleep(100); }
            }
            assertTrue(ready, "Isolated Redis did not become ready; see " + directory);
            ApiClient client = new ApiClient(port);
            client.registerAndLogin("cache_user");
            long userId = client.get("/api/auth/me").number("$.id");
            String key = "learnflow:stats:" + userId;
            assertEquals(0, client.get("/api/stats").number("$.totalTasks"));
            assertNotNull(redis.opsForValue().get(key));
            assertTrue(redis.getExpire(key) > 0 && redis.getExpire(key) <= 60);
            assertEquals(201, client.post("/api/tasks", "{\"title\":\"Cache invalidation\"}").status());
            assertNull(redis.opsForValue().get(key));
            assertEquals(1, client.get("/api/stats").number("$.totalTasks"));
            // 只停止测试自己创建的 Redis，验证缓存离线后普通业务仍能成功。
            process.destroy();
            process.waitFor(5, TimeUnit.SECONDS);
            assertEquals(201, client.post("/api/tasks", "{\"title\":\"Redis offline\"}").status());
            assertEquals(2, client.get("/api/stats").number("$.totalTasks"));
        } finally {
            if (process.isAlive()) { process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS); }
        }
    }
}
