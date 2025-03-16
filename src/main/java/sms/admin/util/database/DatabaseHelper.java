package sms.admin.util.database;

import javax.sql.rowset.CachedRowSet;
import dev.finalproject.App;
import dev.sol.db.DBService;

public class DatabaseHelper {
    private static final DBService DB = App.DB_SMS;
    
    public static void main(String[] args) {
        // Entry point for database operations
        if (args.length > 0) {
            String table = args[0];
            CachedRowSet result = executeQuery(table);
            System.out.println("Query executed for table: " + table);
        } else {
            System.out.println("Usage: DatabaseHelper <table_name>");
        }
    }
    
    public static CachedRowSet executeQuery(String table) {
        return DB.select_all(table);
    }
    
    public static CachedRowSet executeQuery(String table, String customQuery) {
        return DB.select(table);
    }
}
