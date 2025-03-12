package sms.admin.util.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap; // Add proper XSSF imports
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

    private final YearMonth currentMonth = YearMonth.now();

    @Override
    public String getSheetName() {
        return "Attendance";
    }

    @Override
    public List<String> getHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add("ID");
        headers.add("Full Name");

        // Add days of the month
        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = currentMonth.atDay(day);
            if (!AttendanceUtil.isWeekend(date)) {
                headers.add(String.format("%02d", day));
            }
        }

        // Use shorter header names for summary columns
        headers.add("✓"); // Present
        headers.add("✗"); // Absent
        headers.add("½"); // Half Day
        headers.add("E"); // Excused

        return headers;
    }

    @Override
    public List<String> getRowData(Student student) {
        ObservableList<AttendanceLog> logs = DataUtil.createAttendanceLogList();
        List<String> rowData = new ArrayList<>();

        // Basic info
        rowData.add(String.valueOf(student.getStudentID()));
        rowData.add(String.format("%s, %s %s", student.getLastName(), student.getFirstName(), student.getMiddleName()));

        // Daily attendance - use unicode symbols
        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = currentMonth.atDay(day);
            if (!AttendanceUtil.isWeekend(date)) {
                String status = AttendanceUtil.getAttendanceStatus(student, date, logs);
                String symbol = switch (status) {
                    case AttendanceUtil.PRESENT_MARK ->
                        PRESENT_SYMBOL;
                    case AttendanceUtil.ABSENT_MARK ->
                        ABSENT_SYMBOL;
                    case AttendanceUtil.HALF_DAY_MARK ->
                        HALF_DAY_SYMBOL;
                    case AttendanceUtil.EXCUSED_MARK ->
                        EXCUSED_SYMBOL;
                    default ->
                        ABSENT_SYMBOL;
                };
                rowData.add(symbol);
            }
        }

        // Add summary counts
        Map<String, Long> summary = calculateAttendanceSummary(student, logs, currentMonth);
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

        // Count weekdays only
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            if (!AttendanceUtil.isWeekend(date)) {
                String status = AttendanceUtil.getAttendanceStatus(student, date, logs);
                switch (status) {
                    case AttendanceUtil.PRESENT_MARK ->
                        present++;
                    case AttendanceUtil.ABSENT_MARK ->
                        absent++;
                    case AttendanceUtil.HALF_DAY_MARK ->
                        halfDay++;
                    case AttendanceUtil.EXCUSED_MARK ->
                        excused++;
                    default ->
                        absent++; // Count missing records as absent
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
            // Read all bytes from the stream (requires Java 9+)
            byte[] fontBytes = fontStream.readAllBytes();
            // Create the PdfFont using the byte array, specifying the encoding and embedding strategy
            PdfFont font = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

            // Set font for the title
            document.add(new Paragraph(title).setFont(font));

            // Create table with the number of columns based on headers
            Table table = new Table(getHeaders().size());

            // Add headers with font
            for (String header : getHeaders()) {
                table.addCell(new Cell().add(new Paragraph(header).setFont(font)));
            }

            // Add data with font
            for (Student item : items) {
                List<String> rowData = getRowData(item);
                for (String value : rowData) {
                    table.addCell(new Cell().add(new Paragraph(value).setFont(font)));
                }
            }

            // Add the table to the document
            document.add(table);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void writeDataToCsv(PrintWriter writer, ObservableList<Student> items, String title) {
        writeBasicCsv(writer, items, title);
    }
}
