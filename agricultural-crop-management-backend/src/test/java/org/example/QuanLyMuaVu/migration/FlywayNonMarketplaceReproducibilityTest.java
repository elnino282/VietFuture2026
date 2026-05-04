package org.example.QuanLyMuaVu.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

class FlywayNonMarketplaceReproducibilityTest {

    @Test
    void migrateFromCleanSchema_withCoreBaselineAndSeed_succeeds() throws Exception {
        String url = System.getProperty(
                "migration.test.url",
                "jdbc:mysql://127.0.0.1:33306/flyway_repro?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = System.getProperty("migration.test.user", "root");
        String password = System.getProperty("migration.test.password", "rootpass");

        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .baselineOnMigrate(true)
                .load();

        flyway.clean();
        MigrateResult result = flyway.migrate();
        assertTrue(result.success, "Flyway migrate should succeed on clean schema");
        assertEquals("20", result.targetSchemaVersion,
                "Expected schema to reach V20 with non-marketplace seed");

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            assertEquals(1L, queryLong(
                    connection,
                    "SELECT COUNT(*) FROM information_schema.tables " +
                            "WHERE table_schema = DATABASE() AND table_name = 'users'"));
            assertEquals(1L, queryLong(
                    connection,
                    "SELECT COUNT(*) FROM information_schema.tables " +
                            "WHERE table_schema = DATABASE() AND table_name = 'roles'"));
            assertEquals(1L, queryLong(
                    connection,
                    "SELECT COUNT(*) FROM information_schema.tables " +
                            "WHERE table_schema = DATABASE() AND table_name = 'user_preferences'"));
            assertEquals(1L, queryLong(
                    connection,
                    "SELECT COUNT(*) FROM information_schema.tables " +
                            "WHERE table_schema = DATABASE() AND table_name = 'documents'"));

            assertEquals(4L, queryLong(connection,
                    "SELECT COUNT(*) FROM roles WHERE role_code IN ('ADMIN','FARMER','EMPLOYEE','BUYER')"));
            assertEquals(4L, queryLong(connection,
                    "SELECT COUNT(*) FROM users WHERE user_name IN ('admin','farmer','employee','buyer')"));
            assertTrue(queryLong(connection, "SELECT COUNT(*) FROM crops WHERE crop_name = 'Rice'") >= 1L);
            assertTrue(queryLong(connection, "SELECT COUNT(*) FROM farms WHERE farm_name = 'Demo Farm'") >= 1L);
            assertTrue(queryLong(connection, "SELECT COUNT(*) FROM seasons WHERE season_name = 'Spring Demo 2026'") >= 1L);
            assertTrue(queryLong(connection, "SELECT COUNT(*) FROM warehouses WHERE name = 'Input Warehouse Demo'") >= 1L);
            assertTrue(queryLong(connection, "SELECT COUNT(*) FROM supply_lots WHERE batch_code = 'LOT-DEMO-001'") >= 1L);
            assertTrue(queryLong(connection, "SELECT COUNT(*) FROM documents WHERE title = 'Rice Planting Guide'") >= 1L);

            assertEquals(0L, queryLong(
                    connection,
                    "SELECT COUNT(*) FROM information_schema.tables " +
                            "WHERE table_schema = DATABASE() AND table_name LIKE 'marketplace\\_%' ESCAPE '\\\\'"));
        }
    }

    private long queryLong(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
