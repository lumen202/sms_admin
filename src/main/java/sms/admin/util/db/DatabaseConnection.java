package sms.admin.util.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class for managing a single shared database connection.
 * 
 * <p>
 * This class provides a static method to get a reusable {@link Connection}
 * instance,
 * which connects to a MySQL database. It also provides a method to close the
 * connection
 * when it is no longer needed.
 * </p>
 */
public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static Connection connection;

    /**
     * Returns a shared connection to the database. If the connection is not already
     * open,
     * it attempts to create one using the specified connection URL, user, and
     * password.
     *
     * @return an open {@link Connection} to the database
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                String url = "jdbc:mysql://192.168.254.108:3306/student_management_system_db";
                String user = "remote_user";
                String password = ""; // Add your password if needed

                connection = DriverManager.getConnection(
                        url + "?allowPublicKeyRetrieval=true&useSSL=false",
                        user,
                        password);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to establish database connection", e);
                throw e;
            }
        }
        return connection;
    }

    /**
     * Closes the shared database connection if it is open.
     * Logs a warning if an error occurs during closure.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }
}
