package sms.admin.util.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseChangeListener {
    private static final Logger LOGGER = Logger.getLogger(DatabaseChangeListener.class.getName());
    private final Connection connection;
    private final List<DatabaseChangeHandler> changeHandlers;
    private final ScheduledExecutorService scheduler;
    private long lastCheckTimestamp;

    public interface DatabaseChangeHandler {
        void onDatabaseChange(String tableName, String changeType);
    }

    public DatabaseChangeListener(Connection connection) {
        this.connection = connection;
        this.changeHandlers = new ArrayList<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.lastCheckTimestamp = System.currentTimeMillis();
        initializeChangeTracking();
    }

    private void initializeChangeTracking() {
        try {
            // Create change tracking table if it doesn't exist
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS db_changes_log (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    table_name VARCHAR(100) NOT NULL,
                    change_type VARCHAR(20) NOT NULL,
                    change_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """;
            try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
                stmt.execute();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize change tracking", e);
        }
    }

    public void startListening(int intervalSeconds) {
        scheduler.scheduleAtFixedRate(this::checkForChanges, 
            intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    public void addChangeHandler(DatabaseChangeHandler handler) {
        changeHandlers.add(handler);
    }

    private void checkForChanges() {
        try {
            String query = """
                SELECT table_name, change_type, change_timestamp 
                FROM db_changes_log 
                WHERE change_timestamp > ? 
                ORDER BY change_timestamp
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setTimestamp(1, new java.sql.Timestamp(lastCheckTimestamp));
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    String changeType = rs.getString("change_type");
                    lastCheckTimestamp = rs.getTimestamp("change_timestamp").getTime();

                    // Notify all handlers
                    for (DatabaseChangeHandler handler : changeHandlers) {
                        handler.onDatabaseChange(tableName, changeType);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking for database changes", e);
        }
    }

    public void logChange(String tableName, String changeType) {
        try {
            String insertSQL = """
                INSERT INTO db_changes_log (table_name, change_type) 
                VALUES (?, ?)
            """;
            try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
                stmt.setString(1, tableName);
                stmt.setString(2, changeType);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to log database change", e);
        }
    }
}
