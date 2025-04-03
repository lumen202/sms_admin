package sms.admin.util.db;

import java.sql.Connection;
import java.util.concurrent.Callable;

public class DatabaseRetryHelper {
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    public static <T> T withRetry(Callable<T> operation) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return operation.call();
            } catch (Exception e) {
                lastException = e;
                System.err.println("Database operation failed (attempt " + attempt + "/" + MAX_RETRIES + "): " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }
        throw lastException;
    }

    public static Connection getConnectionWithRetry() throws Exception {
        return withRetry(() -> DatabaseConnection.getConnection());
    }
}
