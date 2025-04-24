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

/**
 * Utility class for configuring and managing table columns in the attendance
 * view.
 * Provides methods to create day-specific columns, configure responsive
 * layouts, adjust column widths,
 * and update column styles dynamically based on table size.
 */
public class TableColumnUtil {

    private static final double MIN_COLUMN_WIDTH = 30.0; // Minimum width for any column
    public static final double DEFAULT_COLUMN_WIDTH = 120.0; // Default width for day columns
    public static final double MIN_WEEK_WIDTH = 100.0; // Minimum width for week columns
    public static final double MIN_DAY_WIDTH = 30.0; // Minimum width for day columns

    private static final double MIN_FONT_SIZE = 8.0; // Minimum font size for column text
    private static final double MAX_FONT_SIZE = 14.0; // Maximum font size for column text
    private static final double MIN_CELL_HEIGHT = 24.0; // Minimum cell height
    private static final double MAX_CELL_HEIGHT = 40.0; // Maximum cell height

    private static TableColumn<Student, String> nameColumnReference; // Reference to the name column for alignment

    /**
     * Creates a table column for a specific date, displaying attendance status for
     * each student.
     *
     * @param date  The date for the column.
     * @param logs  The list of attendance logs to determine status.
     * @param width The preferred width of the column.
     * @return The configured TableColumn, or null if date or logs are invalid.
     */
    public static TableColumn<Student, String> createDayColumn(
            LocalDate date,
            ObservableList<AttendanceLog> logs,
            double width) {
        if (date == null || logs == null) {
            return null;
        }

        double effectiveWidth = (width <= 0) ? DEFAULT_COLUMN_WIDTH : width;

        // Create column with header showing day of month and day initial (e.g., "15M"
        // for Monday the 15th)
        TableColumn<Student, String> column = new TableColumn<>(
                String.format("%d%s", date.getDayOfMonth(),
                        CommonAttendanceUtil.getDayInitial(date.getDayOfWeek())));

        // Map student IDs to their attendance logs for quick lookup
        Map<Integer, AttendanceLog> studentLogs = logs.stream()
                .filter(log -> log != null && log.getStudentID() != null)
                .collect(Collectors.toMap(
                        log -> log.getStudentID().getStudentID(),
                        log -> log,
                        (a, b) -> b));

        // Set cell value factory to display attendance status
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
     * Configures a responsive layout for the attendance table, adjusting column
     * widths and styles dynamically.
     *
     * @param table       The TableView to configure.
     * @param idColumn    The column for student IDs.
     * @param nameColumn  The column for student names.
     * @param monthColumn The parent column containing week and day columns.
     */
    public static void configureResponsiveLayout(TableView<Student> table,
            TableColumn<Student, Integer> idColumn,
            TableColumn<Student, String> nameColumn,
            TableColumn<Student, ?> monthColumn) {

        nameColumnReference = nameColumn;
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Adjust widths and styles when table width changes
        table.widthProperty().addListener((obs, oldVal, newVal) -> {
            adjustColumnWidths(table, idColumn, nameColumn, monthColumn);
            updateColumnStyles(table, 12.0);
        });

        // Update styles when table height changes
        table.heightProperty().addListener((obs, oldVal, newVal) -> updateColumnStyles(table, 12.0));

        configureBasicColumns(idColumn, nameColumn, table.getWidth());
        adjustColumnWidths(table, idColumn, nameColumn, monthColumn);
        updateColumnStyles(table, 12.0);
    }

    /**
     * Configures the basic columns (ID and name) with appropriate widths and
     * styles.
     *
     * @param idColumn   The column for student IDs.
     * @param nameColumn The column for student names.
     * @param tableWidth The current width of the table.
     */
    public static void configureBasicColumns(
            TableColumn<Student, Integer> idColumn,
            TableColumn<Student, String> nameColumn,
            double tableWidth) {

        double minTableWidth = 800; // Minimum expected table width
        double maxTableWidth = 2000; // Maximum expected table width

        // ID Column - Fixed width
        idColumn.setPrefWidth(40);
        idColumn.setMinWidth(40);
        idColumn.setMaxWidth(80);
        idColumn.setStyle("-fx-alignment: CENTER;");

        // Name Column - Dynamic width based on table width
        double nameColumnPercentage = Math.max(0.15, Math.min(0.25, tableWidth / maxTableWidth));
        double nameColumnWidth = Math.max(120, tableWidth * nameColumnPercentage);
        nameColumnWidth = Math.min(nameColumnWidth, 300); // Cap maximum width

        nameColumn.setPrefWidth(nameColumnWidth);
        nameColumn.setMinWidth(120);
        nameColumn.setMaxWidth(nameColumnWidth * 1.2);
        nameColumn.setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 10;");
    }

    /**
     * Adjusts the widths of all columns based on the available table width.
     *
     * @param table       The TableView to adjust.
     * @param idColumn    The column for student IDs.
     * @param nameColumn  The column for student names.
     * @param monthColumn The parent column containing week and day columns.
     */
    public static void adjustColumnWidths(TableView<Student> table,
            TableColumn<Student, ?> idColumn,
            TableColumn<Student, ?> nameColumn,
            TableColumn<Student, ?> monthColumn) {

        double totalWidth = table.getWidth();
        if (totalWidth <= 0)
            return;

        double fixedWidth = idColumn.getWidth() + nameColumn.getWidth();
        double availableWidth = totalWidth - fixedWidth - 40;

        adjustMonthColumn(monthColumn, availableWidth);
    }

    /**
     * Adjusts the widths of the month column and its nested week and day columns.
     *
     * @param monthColumn    The parent month column.
     * @param availableWidth The width available for distribution.
     */
    private static void adjustMonthColumn(TableColumn<Student, ?> monthColumn, double availableWidth) {
        if (monthColumn.getColumns().isEmpty())
            return;

        double totalLeafCount = countLeafColumns(monthColumn);
        double baseWidth = availableWidth / totalLeafCount;

        monthColumn.getColumns().forEach(weekColumn -> {
            double weekLeafCount = countLeafColumns(weekColumn);
            double weekWidth = weekLeafCount * baseWidth;

            weekColumn.setPrefWidth(weekWidth);
            weekColumn.setMinWidth(weekWidth * 0.8);
            weekColumn.setMaxWidth(weekWidth * 1.2);

            weekColumn.getColumns().forEach(dayColumn -> {
                dayColumn.setPrefWidth(baseWidth);
                dayColumn.setMinWidth(Math.max(MIN_DAY_WIDTH, baseWidth * 0.7));
                dayColumn.setMaxWidth(baseWidth * 1.3);
            });
        });
    }

    /**
     * Updates the styles of all columns, including font size and cell height, based
     * on table size.
     *
     * @param table        The TableView to style.
     * @param baseFontSize The base font size to scale from.
     */
    public static void updateColumnStyles(TableView<Student> table, double baseFontSize) {
        double tableWidth = table.getWidth();
        int totalColumns = countAllLeafColumns(table);

        // Scale font size based on table width
        double fontSize = Math.min(
                MAX_FONT_SIZE,
                Math.max(MIN_FONT_SIZE, baseFontSize * (tableWidth / 1200)));

        // Scale cell height based on font size
        double cellHeight = MIN_CELL_HEIGHT +
                (fontSize - MIN_FONT_SIZE) * (MAX_CELL_HEIGHT - MIN_CELL_HEIGHT) /
                        (MAX_FONT_SIZE - MIN_FONT_SIZE);

        table.setFixedCellSize(cellHeight);

        // Apply styles to all columns
        table.getColumns().forEach(column -> {
            String style = String.format(
                    "-fx-font-size: %.1fpx; -fx-alignment: %s; -fx-padding: 2px;",
                    fontSize,
                    getColumnAlignment(column));

            column.setStyle(style);
            applyStyleToNestedColumns(column, style);
        });
    }

    /**
     * Determines the alignment for a column, using CENTER-LEFT for the name column
     * and CENTER for others.
     *
     * @param column The column to check.
     * @return The alignment style string.
     */
    private static String getColumnAlignment(TableColumn<?, ?> column) {
        return column == nameColumnReference ? "CENTER-LEFT" : "CENTER";
    }

    /**
     * Recursively applies a style to a column and its nested columns.
     *
     * @param column The column to style.
     * @param style  The CSS style string to apply.
     */
    private static void applyStyleToNestedColumns(TableColumn<?, ?> column, String style) {
        column.getColumns().forEach(child -> {
            child.setStyle(style);
            applyStyleToNestedColumns(child, style);
        });
    }

    /**
     * Counts the number of leaf columns (columns with no sub-columns) under a given
     * column.
     *
     * @param column The column to count.
     * @return The number of leaf columns.
     */
    private static int countLeafColumns(TableColumn<?, ?> column) {
        if (column.getColumns().isEmpty()) {
            return 1;
        }
        return column.getColumns().stream()
                .mapToInt(TableColumnUtil::countLeafColumns)
                .sum();
    }

    /**
     * Counts the total number of leaf columns in the table.
     *
     * @param table The TableView to count.
     * @return The total number of leaf columns.
     */
    private static int countAllLeafColumns(TableView<Student> table) {
        return table.getColumns().stream()
                .mapToInt(TableColumnUtil::countLeafColumns)
                .sum();
    }
}