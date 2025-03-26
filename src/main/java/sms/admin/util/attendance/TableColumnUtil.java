package sms.admin.util.attendance;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import dev.finalproject.models.Student;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import dev.finalproject.models.AttendanceLog;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

public class TableColumnUtil {
    private static final double MIN_COLUMN_WIDTH = 30.0; // Increased from 25.0
    private static final double DEFAULT_COLUMN_WIDTH = 120.0;
    public static final double MIN_WEEK_WIDTH = 100.0;  // Reduced from 120.0
    public static final double MIN_DAY_WIDTH = 30.0;    // Increased from 25.0
    
    private static final double MIN_FONT_SIZE = 8.0;
    private static final double MAX_FONT_SIZE = 14.0;
    private static final double MIN_CELL_HEIGHT = 24.0;
    private static final double MAX_CELL_HEIGHT = 40.0;

    public static TableColumn<Student, String> createDayColumn(
            LocalDate date,
            ObservableList<AttendanceLog> logs,
            double width) {
        if (date == null || logs == null) return null;
        
        TableColumn<Student, String> column = new TableColumn<>(
            String.format("%d%s", date.getDayOfMonth(), 
            CommonAttendanceUtil.getDayInitial(date.getDayOfWeek()))
        );
        
        Map<Integer, AttendanceLog> studentLogs = logs.stream()
            .filter(log -> log != null && log.getStudentID() != null)
            .collect(Collectors.toMap(
                log -> log.getStudentID().getStudentID(),
                log -> log,
                (a, b) -> b
            ));
            
        column.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            if (student != null) {
                AttendanceLog log = studentLogs.get(student.getStudentID());
                return new SimpleStringProperty(CommonAttendanceUtil.computeAttendanceStatus(log));
            }
            return new SimpleStringProperty(CommonAttendanceUtil.ABSENT_MARK);
        });
        
        column.setMinWidth(width);
        column.setPrefWidth(width);
        column.setMaxWidth(width * 1.5);
        column.setResizable(false);
        column.setStyle("-fx-alignment: CENTER;");
        
        return column;
    }

    public static void configureBasicColumns(
            TableColumn<Student, Integer> idColumn,
            TableColumn<Student, String> nameColumn,
            double tableWidth) {
        
        idColumn.setPrefWidth(44);
        idColumn.setMinWidth(44);
        idColumn.setMaxWidth(44);
        idColumn.setResizable(false);
        idColumn.setStyle("-fx-alignment: CENTER;");

        nameColumn.setPrefWidth(300);
        nameColumn.setMinWidth(300);
        nameColumn.setMaxWidth(400);
        nameColumn.setStyle("-fx-alignment: CENTER-LEFT;");
    }

    public static double calculateDayColumnWidth(double availableWidth, int totalDays) {
        return Math.max(MIN_COLUMN_WIDTH, availableWidth / Math.max(totalDays, 1));
    }

    public static void adjustColumnWidths(TableView<Student> table, 
            TableColumn<Student, ?> idColumn,
            TableColumn<Student, ?> nameColumn,
            TableColumn<Student, ?> monthColumn) {
        
        double availableWidth = table.getWidth() - 30 - 130 - 20; // ID width + Name width + padding
        if (availableWidth <= 0) return;

        int totalLeafColumns = countLeafColumns(monthColumn);
        if (totalLeafColumns == 0) return;

        double leafWidth = Math.max(30, availableWidth / (totalLeafColumns * 1.2)); // Reduced division factor
        
        monthColumn.getColumns().forEach(weekCol -> {
            int weekDays = countLeafColumns(weekCol);
            double weekWidth = weekDays * leafWidth * 1.1; // Reduced multiplier
            
            weekCol.setPrefWidth(weekWidth);
            weekCol.setMinWidth(weekWidth * 0.95); // Increased minimum
            weekCol.setMaxWidth(weekWidth * 1.1);
            
            weekCol.getColumns().forEach(dayCol -> {
                int dayColumns = countLeafColumns(dayCol);
                double dayWidth = dayColumns * leafWidth;
                
                dayCol.setPrefWidth(dayWidth);
                dayCol.setMinWidth(dayWidth * 0.9);
                dayCol.setMaxWidth(dayWidth * 1.2);
                
                dayCol.getColumns().forEach(dateCol -> {
                    dateCol.setPrefWidth(leafWidth);
                    dateCol.setMinWidth(leafWidth * 0.9);
                    dateCol.setMaxWidth(leafWidth * 1.2);
                });
            });
        });
        
        monthColumn.setPrefWidth(availableWidth);
    }

    private static int countLeafColumns(TableColumn<?, ?> column) {
        if (column.getColumns().isEmpty()) {
            return 1;
        }
        return column.getColumns().stream()
            .mapToInt(TableColumnUtil::countLeafColumns)
            .sum();
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
