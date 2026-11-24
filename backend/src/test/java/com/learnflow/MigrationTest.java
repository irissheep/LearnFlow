package com.learnflow;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MigrationTest {
    @Test
    void upgradingLegacySchemaPreservesExistingTaskWithoutAssigningOwnership() throws Exception {
        String url = "jdbc:h2:mem:legacy-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE learning_task (id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(200) NOT NULL, status VARCHAR(20) NOT NULL, created_at DATETIME, updated_at DATETIME)");
            statement.execute("INSERT INTO learning_task (title,status) VALUES ('Legacy task','TODO')");
        }
        Flyway.configure().dataSource(url, "sa", "").baselineOnMigrate(true).baselineVersion("0")
                .locations("classpath:db/migration").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT title, user_id FROM learning_task WHERE id=1")) {
            assertTrue(result.next());
            assertEquals("Legacy task", result.getString("title"));
            assertNull(result.getObject("user_id"));
        }
    }
}
