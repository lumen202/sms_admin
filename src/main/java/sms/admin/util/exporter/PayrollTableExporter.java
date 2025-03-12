package sms.admin.util.exporter;

import dev.finalproject.models.Student;
import dev.finalproject.models.AttendanceLog;
import javafx.collections.ObservableList;
import org.apache.poi.ss.usermodel.*;
import com.itextpdf.layout.Document;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.mock.DataUtil;

public class PayrollTableExporter extends BaseTableExporter<Student> {

    private static final String PESO = "₱";
    private static final String DAY_SUFFIX = " Day/s";
    private static final String HALF_DAY_TEXT = " 1/2 Day/s";

    @Override
    public String getSheetName() {
        return "Payroll";
    }

    @Override
    public List<String> getHeaders() {
        return Arrays.asList("ID", "Full Name", "Total Days", "Fare", "Total Amount");
    }

    @Override
    public List<String> getRowData(Student student) {
        ObservableList<AttendanceLog> logs = DataUtil.createAttendanceLogList();
        YearMonth currentMonth = YearMonth.now();
        double totalDays = calculateMonthTotalDays(student, logs, currentMonth);
        double fare = student.getFare();
        double totalAmount = totalDays * fare;

        return Arrays.asList(
                String.valueOf(student.getStudentID()),
                String.format("%s, %s %s", student.getLastName(), student.getFirstName(), student.getMiddleName()),
                formatDays(totalDays),
                PESO + String.format("%,.2f", fare), // Added peso sign and thousands separator
                PESO + String.format("%,.2f", totalAmount) // Added peso sign and thousands separator
        );
    }

    private String formatDays(double days) {
        if (days == Math.floor(days)) {
            // For whole numbers, show without decimal plus "Day/s"
            return String.valueOf((int) days) + DAY_SUFFIX;
        } else {
            // For half days, show as "X 1/2 Day/s"
            int wholePart = (int) Math.floor(days);
            return wholePart + HALF_DAY_TEXT;
        }
    }

    private double calculateMonthTotalDays(Student student, ObservableList<AttendanceLog> logs, YearMonth month) {
        double totalDays = 0;

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            if (!AttendanceUtil.isWeekend(date)) {
                String status = AttendanceUtil.getAttendanceStatus(student, date, logs);
                switch (status) {
                    case AttendanceUtil.PRESENT_MARK, AttendanceUtil.EXCUSED_MARK -> totalDays += 1.0;
                    case AttendanceUtil.HALF_DAY_MARK -> totalDays += 0.5;
                }
            }
        }

        return totalDays;
    }

    @Override
    public void writeDataToWorkbook(Workbook workbook, ObservableList<Student> items, String title) {
        Sheet sheet = workbook.createSheet(getSheetName());

        // Create currency style
        CellStyle currencyStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        currencyStyle.setDataFormat(format.getFormat("₱#,##0.00"));

        writeBasicSheet(workbook, sheet, items, title);

        // Post-process to apply currency formatting
        int fareColumnIndex = 3; // Index of Fare column
        int amountColumnIndex = 4; // Index of Total Amount column

        for (Row row : sheet) {
            if (row.getRowNum() > 1) { // Skip header and title rows
                Cell fareCell = row.getCell(fareColumnIndex);
                Cell amountCell = row.getCell(amountColumnIndex);
                if (fareCell != null)
                    fareCell.setCellStyle(currencyStyle);
                if (amountCell != null)
                    amountCell.setCellStyle(currencyStyle);
            }
        }
    }

    @Override
    public void writeDataToPdf(Document document, ObservableList<Student> items, String title) {
        writeBasicPdf(document, items, title);
    }

    @Override
    public void writeDataToCsv(PrintWriter writer, ObservableList<Student> items, String title) {
        writeBasicCsv(writer, items, title);
    }
}
