package sms.admin.util;

import javax.sql.rowset.CachedRowSet;

import dev.finalproject.App;
import dev.sol.db.DBParam;
import dev.sol.db.DBService;

public class DatabaseManager {
    private static final DBService DB = App.DB_SMS;

    // Since DBService doesn't support transactions, we'll make these no-ops for now
    public static void beginTransaction() throws Exception {
        // No direct transaction support in DBService
    }

    public static void commitTransaction() throws Exception {
        // No direct transaction support in DBService
    }

    public static void rollbackTransaction() throws Exception {
        // No direct transaction support in DBService
    }

    // Add utility methods that match what DBService provides
    public static void insert(String table, DBParam... params) {
        DB.insert(table, params);
    }

    public static CachedRowSet select(String table, DBParam... params) {
        return DB.select(table, params);
    }

    public static void update(String table, DBParam id, DBParam... params) {
        DB.update(table, id, params);
    }

    public static void delete(String table, DBParam... params) {
        DB.delete(table, params);
    }
}
