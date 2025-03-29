package sms.admin.app.attendance.attendancev2;

import javafx.scene.layout.GridPane;
import java.time.YearMonth;
import java.util.List;
import dev.finalproject.models.Student;

public class AttendanceTableExporter {
    private final YearMonth month;

    public AttendanceTableExporter(YearMonth month) {
        this.month = month;
    }

    public void exportToFormat(GridPane grid, List<Student> students, String format, String filePath) {
        switch (format.toLowerCase()) {
            case "excel" -> exportToExcel(grid, students, filePath);
            case "pdf" -> exportToPdf(grid, students, filePath);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    private void exportToExcel(GridPane grid, List<Student> students, String filePath) {
        // Implement Excel export
    }

    private void exportToPdf(GridPane grid, List<Student> students, String filePath) {
        // Implement PDF export
    }
}
