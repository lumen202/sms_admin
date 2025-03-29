package sms.admin.app.attendance.attendancev2;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import java.time.YearMonth;
import java.time.LocalDate;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

public class AttendanceV2Utils {

    private static final double MIN_COLUMN_WIDTH = 40;  // Slightly increased minimum width
    private static final double NAME_COLUMN_MIN_WIDTH = 200;  // Increased name column width
    private static final double ID_COLUMN_MIN_WIDTH = 60;  // Slightly increased ID column width
    private static final double PADDING = 5;  // Reduced padding for tighter layout

    public static ColumnConstraints createDynamicColumn(double minWidth, double prefWidth, double maxWidth) {
        ColumnConstraints column = new ColumnConstraints();
        column.setHgrow(Priority.SOMETIMES);
        column.setMinWidth(minWidth);
        column.setPrefWidth(prefWidth);
        column.setMaxWidth(maxWidth);
        column.setFillWidth(true);
        return column;
    }

    public static double calculateDateColumnWidth(GridPane grid, int weekdayCount) {
        double totalWidth = grid.getWidth();
        if (totalWidth <= 0) {
            return MIN_COLUMN_WIDTH;
        }

        // Calculate space needed for fixed columns
        double fixedWidth = ID_COLUMN_MIN_WIDTH + NAME_COLUMN_MIN_WIDTH + (PADDING * 3);

        // Calculate remaining width for date columns
        double availableWidth = totalWidth - fixedWidth;

        // Calculate width per date column, ensuring minimum width
        return Math.max(MIN_COLUMN_WIDTH, (availableWidth / weekdayCount) - PADDING);
    }

    public static void resizeGridColumns(GridPane grid, int weekdayCount) {
        double width = grid.getWidth();
        if (width <= 0) {
            return;
        }

        grid.getColumnConstraints().clear();

        // Fixed width columns for ID and Name
        ColumnConstraints idColumn = new ColumnConstraints();
        idColumn.setMinWidth(60);
        idColumn.setPrefWidth(80);
        idColumn.setMaxWidth(100);
        idColumn.setHgrow(Priority.NEVER);

        ColumnConstraints nameColumn = new ColumnConstraints();
        nameColumn.setMinWidth(200);
        nameColumn.setPrefWidth(250);
        nameColumn.setMaxWidth(300);
        nameColumn.setHgrow(Priority.NEVER);

        grid.getColumnConstraints().addAll(idColumn, nameColumn);

        // Calculate remaining width for date columns
        double remainingWidth = Math.max(0, width - idColumn.getPrefWidth() - nameColumn.getPrefWidth());
        double dateColumnWidth = remainingWidth / weekdayCount;

        // Add equal-width columns for dates
        for (int i = 0; i < weekdayCount; i++) {
            ColumnConstraints dateColumn = new ColumnConstraints();
            dateColumn.setMinWidth(40);
            dateColumn.setPrefWidth(dateColumnWidth);
            dateColumn.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(dateColumn);
        }

        grid.applyCss();
        grid.layout();
    }

    public static void distributeRowHeights(GridPane grid, int rowCount) {
        double height = grid.getHeight();
        if (height <= 0) {
            return;
        }

        double rowHeight = Math.max(35, height / (rowCount + 2)); // +2 for headers
        grid.getRowConstraints().forEach(row -> {
            row.setMinHeight(30);
            row.setPrefHeight(rowHeight);
            row.setVgrow(Priority.SOMETIMES);
        });
    }

    public static Node findGridCell(GridPane grid, int col, int row) {
        return grid.getChildren().stream()
                .filter(node -> GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row)
                .findFirst()
                .orElse(null);
    }

    public static class DateRange {

        public final LocalDate start;
        public final LocalDate end;

        public DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }

    public static DateRange getMonthDateRange(YearMonth month) {
        return new DateRange(month.atDay(1), month.atEndOfMonth());
    }
}
