package sms.admin.util.attendance;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import dev.finalproject.models.Student;
import javafx.collections.ObservableList;

public class TableColumnUtil {
    public static final double MIN_WEEK_WIDTH = 200.0;
    public static final double MIN_DAY_WIDTH = 40.0;
    
    private static final double MIN_FONT_SIZE = 8.0;
    private static final double MAX_FONT_SIZE = 14.0;
    private static final double MIN_CELL_HEIGHT = 24.0;
    private static final double MAX_CELL_HEIGHT = 40.0;

    public static void adjustColumnWidths(TableView<Student> table, 
            TableColumn<Student, ?> idColumn,
            TableColumn<Student, ?> nameColumn,
            TableColumn<Student, ?> monthColumn) {
        
        // Calculate available width for month columns
        double availableWidth = table.getWidth() - idColumn.getWidth() - nameColumn.getWidth();
        int weekCount = monthColumn.getColumns().size();
        
        if (weekCount > 0) {
            // Force fit all columns within available width
            double weekWidth = Math.max(availableWidth / weekCount, MIN_WEEK_WIDTH);
            double totalWeekWidth = weekWidth * weekCount;
            
            // If total width exceeds available space, recalculate to fit
            if (totalWeekWidth > availableWidth) {
                weekWidth = availableWidth / weekCount;
            }
            
            distributeWeekWidths(monthColumn.getColumns(), weekWidth, weekCount);
        }
    }
    
    private static void distributeWeekWidths(ObservableList<TableColumn<Student, ?>> weekColumns, 
            double weekWidth, int weekCount) {
        weekColumns.forEach(weekCol -> {
            weekCol.setMinWidth(weekWidth * 0.9);  // Allow slight shrinking
            weekCol.setPrefWidth(weekWidth);
            weekCol.setMaxWidth(weekWidth * 1.1);  // Allow slight growing
            
            ObservableList<TableColumn<Student, ?>> dayColumns = weekCol.getColumns();
            if (!dayColumns.isEmpty()) {
                // Distribute day widths proportionally within week width
                double dayWidth = weekWidth / dayColumns.size();
                distributeDayWidths(dayColumns, dayWidth);
            }
        });
    }
    
    private static void distributeDayWidths(ObservableList<TableColumn<Student, ?>> dayColumns, double dayWidth) {
        dayColumns.forEach(dayCol -> {
            dayCol.setMinWidth(dayWidth * 0.9);    // Allow slight shrinking
            dayCol.setPrefWidth(dayWidth);
            dayCol.setMaxWidth(dayWidth * 1.1);    // Allow slight growing
            
            ObservableList<TableColumn<Student, ?>> dateColumns = dayCol.getColumns();
            if (!dateColumns.isEmpty()) {
                double dateWidth = dayWidth / dateColumns.size();
                distributeDateWidths(dateColumns, dateWidth);
            }
        });
    }
    
    private static void distributeDateWidths(ObservableList<TableColumn<Student, ?>> dateColumns, double dayWidth) {
        dateColumns.forEach(dateCol -> {
            dateCol.setMinWidth(dayWidth * 0.9);   // Allow slight shrinking
            dateCol.setPrefWidth(dayWidth);
            dateCol.setMaxWidth(dayWidth * 1.1);   // Allow slight growing
        });
    }

    public static void updateColumnStyles(TableView<Student> table, double fontSize) {
        // Calculate cell height based on font size
        double cellHeight = calculateCellHeight(fontSize);
        String style = String.format("-fx-font-size: %.1fpx; -fx-alignment: CENTER; -fx-padding: 2px;", fontSize);
        
        // Set row height for the entire table
        table.setFixedCellSize(cellHeight);
        
        // Apply styles to all columns
        table.getColumns().forEach(column -> {
            column.setStyle(style);
            if (column instanceof TableColumn) {
                applyStyleToNestedColumns((TableColumn<?,?>) column, style, cellHeight);
            }
        });
    }

    private static void applyStyleToNestedColumns(TableColumn<?,?> column, String style, double cellHeight) {
        column.getColumns().forEach(subColumn -> {
            subColumn.setStyle(style);
            // Adjust minimum width based on cell height to maintain aspect ratio
            double minWidth = Math.max(MIN_DAY_WIDTH, cellHeight * 1.5);
            subColumn.setMinWidth(minWidth);
            
            if (!subColumn.getColumns().isEmpty()) {
                applyStyleToNestedColumns(subColumn, style, cellHeight);
            }
        });
    }

    private static double calculateCellHeight(double fontSize) {
        // Calculate cell height proportionally to font size
        double ratio = (fontSize - MIN_FONT_SIZE) / (MAX_FONT_SIZE - MIN_FONT_SIZE);
        return MIN_CELL_HEIGHT + (ratio * (MAX_CELL_HEIGHT - MIN_CELL_HEIGHT));
    }

    public static double calculateDynamicFontSize(double tableWidth, int columnCount) {
        if (tableWidth <= 0 || columnCount <= 0) return MIN_FONT_SIZE;

        double availableWidth = tableWidth / columnCount;
        double scaleFactor = availableWidth / 100.0;
        double fontSize = 11.0 * scaleFactor;
        
        return Math.min(MAX_FONT_SIZE, Math.max(MIN_FONT_SIZE, fontSize));
    }
}
