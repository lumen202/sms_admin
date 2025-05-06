package sms.admin.util.db;

import java.sql.Connection;
import java.util.concurrent.Callable;

/**
 * Utility class for retrying database operations with exponential backoff.
 * 
 * <p>
 * This helper can be used to wrap database calls that may fail intermittently,
 * retrying them a limited number of times before giving up.
 * </p>
 */
public class DatabaseRetryHelper {
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    /**
     * Executes a database operation with retry logic.
     *
     * <p>
     * The operation will be attempted up to {@code MAX_RETRIES} times.
     * After each failed attempt (except the last), the method waits for an
     * increasing delay
     * before retrying. The delay is calculated as
     * {@code RETRY_DELAY_MS * attemptNumber}.
     * </p>
     *
     * @param <T>       the type of result returned by the operation
     * @param operation the operation to perform, as a {@link Callable}
     * @return the result of the successful operation
     * @throws Exception if all retry attempts fail
     */
    public static <T> T withRetry(Callable<T> operation) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return operation.call();
            } catch (Exception e) {
                lastException = e;
                System.err.println(
                        "Database operation failed (attempt " + attempt + "/" + MAX_RETRIES + "): " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }
        throw lastException;
    }

    /**
     * Attempts to establish a database connection using retry logic.
     *
     * <p>
     * This method wraps {@link DatabaseConnection#getConnection()} with
     * {@link #withRetry(Callable)},
     * retrying the connection up to {@code MAX_RETRIES} times if it fails.
     * </p>
     *
     * @return an established {@link Connection} to the database
     * @throws Exception if the connection could not be established after all
     *                   retries
     */
    public static Connection getConnectionWithRetry() throws Exception {
        return withRetry(() -> DatabaseConnection.getConnection());
    }
}
