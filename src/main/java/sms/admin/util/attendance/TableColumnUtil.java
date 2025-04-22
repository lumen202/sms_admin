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

    private static TableColumn<Student, String> nameColumnReference;

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

    public static void configureResponsiveLayout(TableView<Student> table,
            TableColumn<Student, Integer> idColumn,
            TableColumn<Student, String> nameColumn,
            TableColumn<Student, ?> monthColumn) {

        nameColumnReference = nameColumn;
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.widthProperty().addListener((obs, oldVal, newVal) -> {
            adjustColumnWidths(table, idColumn, nameColumn, monthColumn);
            updateColumnStyles(table, 12.0);
        });

        table.heightProperty().addListener((obs, oldVal, newVal) -> updateColumnStyles(table, 12.0));

        configureBasicColumns(idColumn, nameColumn, table.getWidth());
        adjustColumnWidths(table, idColumn, nameColumn, monthColumn);
        updateColumnStyles(table, 12.0);
    }

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

    public static void updateColumnStyles(TableView<Student> table, double baseFontSize) {
        double tableWidth = table.getWidth();
        int totalColumns = countAllLeafColumns(table);

        double fontSize = Math.min(
                MAX_FONT_SIZE,
                Math.max(MIN_FONT_SIZE, baseFontSize * (tableWidth / 1200)));

        double cellHeight = MIN_CELL_HEIGHT +
                (fontSize - MIN_FONT_SIZE) * (MAX_CELL_HEIGHT - MIN_CELL_HEIGHT) /
                        (MAX_FONT_SIZE - MIN_FONT_SIZE);

        table.setFixedCellSize(cellHeight);

        table.getColumns().forEach(column -> {
            String style = String.format(
                    "-fx-font-size: %.1fpx; -fx-alignment: %s; -fx-padding: 2px;",
                    fontSize,
                    getColumnAlignment(column));

            column.setStyle(style);
            applyStyleToNestedColumns(column, style);
        });
    }

    private static String getColumnAlignment(TableColumn<?, ?> column) {
        return column == nameColumnReference ? "CENTER-LEFT" : "CENTER";
    }

    private static void applyStyleToNestedColumns(TableColumn<?, ?> column, String style) {
        column.getColumns().forEach(child -> {
            child.setStyle(style);
            applyStyleToNestedColumns(child, style);
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

    private static int countAllLeafColumns(TableView<Student> table) {
        return table.getColumns().stream()
                .mapToInt(TableColumnUtil::countLeafColumns)
                .sum();
    }
}