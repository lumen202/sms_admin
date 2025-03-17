package sms.admin.util.attendance;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import dev.finalproject.models.Student;
import javafx.collections.ObservableList;

public class TableColumnUtil {
    public static final double MIN_WEEK_WIDTH = 200.0;
    public static final double MIN_DAY_WIDTH = 40.0;
    
    public static void adjustColumnWidths(TableView<Student> table, 
            TableColumn<Student, ?> idColumn,
            TableColumn<Student, ?> nameColumn,
            TableColumn<Student, ?> monthColumn) {
        
        double availableWidth = Math.max(table.getWidth() - idColumn.getWidth() - nameColumn.getWidth() - 2, 400);
        int weekCount = monthColumn.getColumns().size();
        
        if (weekCount > 0) {
            distributeWeekWidths(monthColumn.getColumns(), availableWidth, weekCount);
        }
    }
    
    private static void distributeWeekWidths(ObservableList<TableColumn<Student, ?>> weekColumns, 
            double availableWidth, int weekCount) {
        double weekWidth = Math.max(availableWidth / weekCount, MIN_WEEK_WIDTH);
        
        weekColumns.forEach(weekCol -> {
            weekCol.setMinWidth(weekWidth);
            weekCol.setPrefWidth(weekWidth);
            
            ObservableList<TableColumn<Student, ?>> dayColumns = weekCol.getColumns();
            if (!dayColumns.isEmpty()) {
                distributeDayWidths(dayColumns, weekWidth);
            }
        });
    }
    
    private static void distributeDayWidths(ObservableList<TableColumn<Student, ?>> dayColumns, double weekWidth) {
        double dayWidth = Math.max(weekWidth / dayColumns.size(), MIN_DAY_WIDTH);
        
        dayColumns.forEach(dayCol -> {
            dayCol.setMinWidth(dayWidth);
            dayCol.setPrefWidth(dayWidth);
            
            ObservableList<TableColumn<Student, ?>> dateColumns = dayCol.getColumns();
            if (!dateColumns.isEmpty()) {
                distributeDateWidths(dateColumns, dayWidth);
            }
        });
    }
    
    private static void distributeDateWidths(ObservableList<TableColumn<Student, ?>> dateColumns, double dayWidth) {
        dateColumns.forEach(dateCol -> {
            dateCol.setMinWidth(dayWidth);
            dateCol.setPrefWidth(dayWidth);
            dateCol.setMaxWidth(dayWidth);
        });
    }
}
