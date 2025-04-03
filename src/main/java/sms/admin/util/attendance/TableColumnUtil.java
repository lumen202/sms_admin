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

    private static final double MIN_COLUMN_WIDTH = 30.0;
    public static final double DEFAULT_COLUMN_WIDTH = 120.0;
    public static final double MIN_WEEK_WIDTH = 100.0;
    public static final double MIN_DAY_WIDTH = 30.0;

    private static final double MIN_FONT_SIZE = 8.0;
    private static final double MAX_FONT_SIZE = 14.0;
    private static final double MIN_CELL_HEIGHT = 24.0;
    private static final double MAX_CELL_HEIGHT = 40.0;

    public static TableColumn<Student, String> createDayColumn(
            LocalDate date,
            ObservableList<AttendanceLog> logs,
            double width) {
        if (date == null || logs == null) {
            return null;
        }

        double effectiveWidth = (width <= 0) ? DEFAULT_COLUMN_WIDTH : width;

        TableColumn<Student, String> column = new TableColumn<>(
                String.format("%d%s", date.getDayOfMonth(),
                        CommonAttendanceUtil.getDayInitial(date.getDayOfWeek())));

        Map<Integer, AttendanceLog> studentLogs = logs.stream()
                .filter(log -> log != null && log.getStudentID() != null)
                .collect(Collectors.toMap(
                        log -> log.getStudentID().getStudentID(),
                        log -> log,
                        (a, b) -> b));

        column.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            if (student != null) {
                AttendanceLog log = studentLogs.get(student.getStudentID());
                return new SimpleStringProperty(CommonAttendanceUtil.computeAttendanceStatus(log));
            }
            return new SimpleStringProperty(CommonAttendanceUtil.ABSENT_MARK);
        });

        column.setMinWidth(MIN_DAY_WIDTH);
        column.setPrefWidth(effectiveWidth);
        column.setMaxWidth(effectiveWidth * 1.5);
        column.setResizable(true);
        column.setStyle("-fx-alignment: CENTER;");

        return column;
    }

    /**
     * Dynamically configures basic columns. For full screen widths, the name
     * column will have a larger width.
     */
    public static void configureBasicColumns(
            TableColumn<Student, Integer> idColumn,
            TableColumn<Student, String> nameColumn,
            double tableWidth) {
        // Configure the ID column.
        idColumn.setPrefWidth(30);
        idColumn.setMinWidth(30);
        idColumn.setMaxWidth(50);
        idColumn.setResizable(true);
        idColumn.setStyle("-fx-alignment: CENTER;");

        // Use 15% of the table width for the name column,
        // with a lower minimum width (e.g., 120 pixels).
        double nameColumnWidth = tableWidth * 0.15;
        nameColumnWidth = Math.max(120, nameColumnWidth);

        nameColumn.setPrefWidth(nameColumnWidth);
        nameColumn.setMinWidth(120);
        nameColumn.setMaxWidth(nameColumnWidth * 1.5);
        nameColumn.setResizable(true);
        nameColumn.setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 10;");
    }

    public static double calculateDayColumnWidth(double availableWidth, int totalDays) {
        return Math.max(MIN_DAY_WIDTH, availableWidth / Math.max(totalDays, 1));
    }

    public static void adjustColumnWidths(TableView<Student> table,
            TableColumn<Student, ?> idColumn,
            TableColumn<Student, ?> nameColumn,
            TableColumn<Student, ?> monthColumn) {
        double totalWidth = table.getWidth();
        if (totalWidth <= 0) {
            return;
        }

        // Use actual widths instead of prefWidth to reflect current state
        double fixedWidth = idColumn.getWidth() + nameColumn.getWidth() + 20;
        double availableWidth = Math.max(0, totalWidth - fixedWidth);

        int totalLeafColumns = countLeafColumns(monthColumn);
        if (totalLeafColumns == 0) {
            return;
        }

        double baseWidth = availableWidth / totalLeafColumns;
        double minWidth = Math.max(MIN_DAY_WIDTH, baseWidth * 0.9);
        double maxWidth = baseWidth * 1.5;

        monthColumn.setMinWidth(availableWidth * 0.9);
        monthColumn.setPrefWidth(availableWidth);
        monthColumn.setMaxWidth(availableWidth * 1.1);

        ObservableList<TableColumn<Student, ?>> weekColumns = monthColumn.getColumns();
        weekColumns.forEach(weekCol -> {
            int leafCount = countLeafColumns(weekCol);
            double weekWidth = baseWidth * leafCount;
            weekCol.setMinWidth(Math.max(MIN_WEEK_WIDTH, weekWidth * 0.9));
            weekCol.setPrefWidth(weekWidth);
            weekCol.setMaxWidth(weekWidth * 1.5);
            weekCol.setResizable(true);

            ObservableList<TableColumn<Student, ?>> dayColumns = weekCol.getColumns();
            dayColumns.forEach(dayCol -> {
                int dateCount = dayCol.getColumns().size();
                double dayWidth = dateCount > 0 ? weekWidth / dateCount : weekWidth;
                dayCol.setMinWidth(minWidth);
                dayCol.setPrefWidth(dayWidth);
                dayCol.setMaxWidth(maxWidth);
                dayCol.setResizable(true);

                ObservableList<TableColumn<Student, ?>> dateColumns = dayCol.getColumns();
                dateColumns.forEach(dateCol -> {
                    dateCol.setMinWidth(minWidth);
                    dateCol.setPrefWidth(baseWidth);
                    dateCol.setMaxWidth(maxWidth);
                    dateCol.setResizable(true);
                });
            });
        });
    }

    private static int countLeafColumns(TableColumn<?, ?> column) {
        if (column.getColumns().isEmpty()) {
            return 1;
        }
        return column.getColumns().stream()
                .mapToInt(TableColumnUtil::countLeafColumns)
                .sum();
    }

    /**
     * Dynamically updates column styles. In full screen mode, the name column
     * font size is increased.
     */
    public static void updateColumnStyles(TableView<Student> table, double baseFontSize) {
        int totalColumns = countLeafColumns(table.getColumns().get(0))
                + countLeafColumns(table.getColumns().get(1))
                + countLeafColumns(table.getColumns().get(2));
        double fontSize = calculateDynamicFontSize(table.getWidth(), totalColumns);
        double nameFontSize = fontSize * 1.2;
        if (table.getWidth() >= 1200) {
            // Increase the name column font size more for full screen
            nameFontSize = fontSize * 1.5;
        }

        // Define a larger font for the month column.
        double monthFontSize = fontSize * 1.3;

        double cellHeight = calculateCellHeight(fontSize);
        String defaultStyle = String.format("-fx-font-size: %.1fpx; -fx-alignment: CENTER; -fx-padding: 2px;",
                fontSize);
        String nameStyle = String.format("-fx-font-size: %.1fpx; -fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 10;",
                nameFontSize);
        String monthStyle = String.format("-fx-font-size: %.1fpx; -fx-alignment: CENTER; -fx-padding: 2px;",
                monthFontSize);

        table.setFixedCellSize(cellHeight);

        table.getColumns().forEach(column -> {
            if (column == table.getColumns().get(1)) {
                column.setStyle(nameStyle);
            } else if (column == table.getColumns().get(2)) {
                column.setStyle(monthStyle);
            } else {
                column.setStyle(defaultStyle);
            }
            if (column instanceof TableColumn) {
                String nestedStyle = defaultStyle;
                if (column == table.getColumns().get(1)) {
                    nestedStyle = nameStyle; 
                }else if (column == table.getColumns().get(2)) {
                    nestedStyle = monthStyle;
                }
                applyStyleToNestedColumns((TableColumn<?, ?>) column, nestedStyle, cellHeight);
            }
        });
    }

    private static void applyStyleToNestedColumns(TableColumn<?, ?> column, String style, double cellHeight) {
        column.getColumns().forEach(subColumn -> {
            subColumn.setStyle(style);
            double minWidth = Math.max(MIN_DAY_WIDTH, cellHeight * 1.5);
            subColumn.setMinWidth(minWidth);
            if (!subColumn.getColumns().isEmpty()) {
                applyStyleToNestedColumns(subColumn, style, cellHeight);
            }
        });
    }

    private static double calculateCellHeight(double fontSize) {
        double ratio = (fontSize - MIN_FONT_SIZE) / (MAX_FONT_SIZE - MIN_FONT_SIZE);
        return MIN_CELL_HEIGHT + (ratio * (MAX_CELL_HEIGHT - MIN_CELL_HEIGHT));
    }

    public static double calculateDynamicFontSize(double tableWidth, int columnCount) {
        if (tableWidth <= 0 || columnCount <= 0) {
            return MIN_FONT_SIZE;
        }

        double availableWidthPerColumn = tableWidth / columnCount;
        double scaleFactor = availableWidthPerColumn / 100.0;
        double fontSize = 11.0 * scaleFactor;
        return Math.min(MAX_FONT_SIZE, Math.max(MIN_FONT_SIZE, fontSize));
    }
}
