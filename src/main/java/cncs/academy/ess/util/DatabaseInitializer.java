package cncs.academy.ess.util;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    public static void initialize(BasicDataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            log.info("Initializing database schema...");

            // Users Table
            // Using "IF NOT EXISTS" to avoid errors on restart
            // Updated schema for PBKDF2 + RBAC
            String createUsersTable = """
                        CREATE TABLE IF NOT EXISTS users (
                            id SERIAL PRIMARY KEY,
                            username VARCHAR(255) UNIQUE NOT NULL,
                            password_hash VARCHAR(255) NOT NULL,
                            password_salt VARCHAR(255) NOT NULL,
                            role VARCHAR(50) DEFAULT 'base'
                        );
                    """;
            executeStatement(connection, createUsersTable);

            // Lists Table
            String createListsTable = """
                        CREATE TABLE IF NOT EXISTS lists (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            owner_id INTEGER NOT NULL,
                            FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
                        );
                    """;
            executeStatement(connection, createListsTable);

            // Todos/Items Table
            String createTodosTable = """
                        CREATE TABLE IF NOT EXISTS todos (
                            id SERIAL PRIMARY KEY,
                            description VARCHAR(255) NOT NULL,
                            completed BOOLEAN NOT NULL DEFAULT FALSE,
                            list_id INTEGER NOT NULL,
                            FOREIGN KEY (list_id) REFERENCES lists(id) ON DELETE CASCADE
                        );
                    """;
            executeStatement(connection, createTodosTable);

            // List Shares Table
            String createListSharesTable = """
                        CREATE TABLE IF NOT EXISTS list_shares (
                            list_id INTEGER NOT NULL,
                            user_id INTEGER NOT NULL,
                            PRIMARY KEY (list_id, user_id),
                            FOREIGN KEY (list_id) REFERENCES lists(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        );
                    """;
            executeStatement(connection, createListSharesTable);

            log.info("Database schema initialized successfully.");

        } catch (SQLException e) {
            log.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static void executeStatement(Connection connection, String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
}
