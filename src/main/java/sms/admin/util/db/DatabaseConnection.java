package sms.admin.util.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static Connection connection;
    
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                String url = "jdbc:mysql://192.168.254.108:3306/student_management_system_db";
                String user = "remote_user";
                String password = "";  // Add your password if needed
                connection = DriverManager.getConnection(url + "?allowPublicKeyRetrieval=true&useSSL=false", 
                                                      user, password);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to establish database connection", e);
                throw e;
            }
        }
        return connection;
    }

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
