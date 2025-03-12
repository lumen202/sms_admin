package sms.admin.util.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import javafx.collections.ObservableList;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.mock.DataUtil;

public class AttendanceTableExporter extends BaseTableExporter<Student> {

    private static final String PRESENT_SYMBOL = "\u2713"; // ✓
    private static final String ABSENT_SYMBOL = "\u2717"; // ✗
    private static final String HALF_DAY_SYMBOL = "\u00BD"; // ½
    private static final String EXCUSED_SYMBOL = "E";

    private YearMonth selectedMonth; // Add this field

    public AttendanceTableExporter(YearMonth selectedMonth) {
        this.selectedMonth = selectedMonth;
    }

    @Override
    public String getSheetName() {
        return "Attendance";
    }

    @Override
    public List<String> getHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add("ID");
        headers.add("Full Name");

        // Use selectedMonth instead of currentMonth
        for (int day = 1; day <= selectedMonth.lengthOfMonth(); day++) {
            LocalDate date = selectedMonth.atDay(day);
            if (!AttendanceUtil.isWeekend(date)) {
                headers.add(String.format("%02d", day));
            }
        }

        headers.add("Present");
        headers.add("Absent");
        headers.add("Half Day");
        headers.add("Excused");

        return headers;
    }

    @Override
    public List<String> getRowData(Student student) {
        ObservableList<AttendanceLog> logs = DataUtil.createAttendanceLogList();
        List<String> rowData = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Basic info
        rowData.add(String.valueOf(student.getStudentID()));
        rowData.add(String.format("%s, %s %s", student.getLastName(), student.getFirstName(), student.getMiddleName()));

        // Update the daily attendance loop to use selectedMonth
        for (int day = 1; day <= selectedMonth.lengthOfMonth(); day++) {
            LocalDate date = selectedMonth.atDay(day);
            if (!AttendanceUtil.isWeekend(date)) {
                if (date.isAfter(today)) {
                    rowData.add(""); // Future date - leave empty
                } else {
                    String status = AttendanceUtil.getAttendanceStatus(student, date, logs);
                    String symbol = switch (status) {
                        case AttendanceUtil.PRESENT_MARK -> PRESENT_SYMBOL;
                        case AttendanceUtil.ABSENT_MARK -> ABSENT_SYMBOL;
                        case AttendanceUtil.HALF_DAY_MARK -> HALF_DAY_SYMBOL;
                        case AttendanceUtil.EXCUSED_MARK -> EXCUSED_SYMBOL;
                        default -> ""; // Changed from ABSENT_SYMBOL to empty for missing/future records
                    };
                    rowData.add(symbol);
                }
            }
        }

        // Update summary calculation to use selectedMonth
        Map<String, Long> summary = calculateAttendanceSummary(student, logs, selectedMonth);
        rowData.add(String.valueOf(summary.get("present")));
        rowData.add(String.valueOf(summary.get("absent")));
        rowData.add(String.valueOf(summary.get("halfDay")));
        rowData.add(String.valueOf(summary.get("excused")));

        return rowData;
    }

    private Map<String, Long> calculateAttendanceSummary(Student student, ObservableList<AttendanceLog> logs,
            YearMonth month) {
        Map<String, Long> summary = new HashMap<>();
        long present = 0, absent = 0, halfDay = 0, excused = 0;
        LocalDate today = LocalDate.now();

        // Count weekdays only up to current date
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            if (!AttendanceUtil.isWeekend(date) && !date.isAfter(today)) {
                String status = AttendanceUtil.getAttendanceStatus(student, date, logs);
                switch (status) {
                    case AttendanceUtil.PRESENT_MARK -> present++;
                    case AttendanceUtil.ABSENT_MARK -> absent++;
                    case AttendanceUtil.HALF_DAY_MARK -> halfDay++;
                    case AttendanceUtil.EXCUSED_MARK -> excused++;
                    default -> absent++; // Count missing records as absent
                }
            }
        }

        summary.put("present", present);
        summary.put("absent", absent);
        summary.put("halfDay", halfDay);
        summary.put("excused", excused);

        return summary;
    }

    @Override
    protected void writeDataToWorkbook(Workbook workbook, ObservableList<Student> items, String title) {
        Sheet sheet = workbook.createSheet(getSheetName());

        // Create styles
        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Create font using proper casting
        XSSFFont font = null;
        if (workbook instanceof XSSFWorkbook) {
            font = ((XSSFWorkbook) workbook).createFont();
            font.setFontName("Arial");
        }

        // Create styles with font
        CellStyle defaultStyle = workbook.createCellStyle();
        if (font != null) {
            defaultStyle.setFont(font);
        }
        defaultStyle.setAlignment(HorizontalAlignment.CENTER);

        // Left align style
        CellStyle nameStyle = workbook.createCellStyle();
        if (font != null) {
            nameStyle.setFont(font);
        }
        nameStyle.setAlignment(HorizontalAlignment.LEFT);

        // Set column widths
        sheet.setColumnWidth(0, 10 * 256); // ID column
        sheet.setColumnWidth(1, 40 * 256); // Full Name column

        // Auto-size other columns
        for (int i = 2; i < getHeaders().size(); i++) {
            sheet.setColumnWidth(i, 8 * 256); // Standard width for attendance marks
        }

        // Write the data first
        writeBasicSheet(workbook, sheet, items, title);

        // Apply styles after writing data
        for (Row row : sheet) {
            for (int i = 0; i < row.getLastCellNum(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = row.getCell(i);
                if (cell != null) {
                    if (i == 1) { // Name column
                        cell.setCellStyle(nameStyle);
                    } else { // All other columns
                        cell.setCellStyle(defaultStyle);
                    }
                }
            }
        }
    }

    @Override
    protected void writeDataToPdf(Document document, ObservableList<Student> items, String title) {
        try {
            // Load the .ttf font file as a resource stream
            InputStream fontStream = getClass().getResourceAsStream("/sms/admin/assets/fonts/arialuni.ttf");
            if (fontStream == null) {
                throw new IOException("Font file not found in resources.");
            }
            byte[] fontBytes = fontStream.readAllBytes();
            PdfFont font = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

            // Calculate dynamic font size based on number of columns
            float fontSize = calculateDynamicFontSize(getHeaders().size());

            // Set font for the title with slightly larger size
            document.add(new Paragraph(title).setFont(font).setFontSize(fontSize + 2));
            document.add(new Paragraph("\n")); // Add some spacing

            // Create table with the number of columns based on headers
            Table table = new Table(getHeaders().size());
            table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

            // Add headers with font
            for (String header : getHeaders()) {
                Cell cell = new Cell().add(new Paragraph(header).setFont(font).setFontSize(fontSize));
                cell.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
                table.addCell(cell);
            }

            // Add data with font
            for (Student item : items) {
                List<String> rowData = getRowData(item);
                for (int i = 0; i < rowData.size(); i++) {
                    Cell cell = new Cell().add(new Paragraph(rowData.get(i)).setFont(font).setFontSize(fontSize));
                    // Left align the name column (index 1)
                    if (i == 1) {
                        cell.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.LEFT);
                    } else {
                        cell.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
                    }
                    table.addCell(cell);
                }
            }

            // Add the table to the document
            document.add(table);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float calculateDynamicFontSize(int columnCount) {
        // Base size for few columns
        float baseSize = 10f;

        // Adjust font size based on column count
        if (columnCount <= 15) {
            return baseSize;
        } else if (columnCount <= 20) {
            return 8f;
        } else if (columnCount <= 25) {
            return 7f;
        } else if (columnCount <= 30) {
            return 6f;
        } else {
            return 5f; // Minimum size
        }
    }

    @Override
    protected void writeDataToCsv(PrintWriter writer, ObservableList<Student> items, String title) {
        writeBasicCsv(writer, items, title);
    }
}
