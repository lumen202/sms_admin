package sms.admin.util.exporter.exporterv2;

import javafx.collections.ObservableList;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.exporter.BaseTableExporter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import com.itextpdf.layout.Document;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class DetailedPayrollExporter extends BaseTableExporter<Student> {

    private static final String PESO = "₱";
    private static final String CHECKMARK = "\u2713"; // ✓
    private final YearMonth period;
    private final ObservableList<AttendanceLog> attendanceLogs; // Assumed external source

    public DetailedPayrollExporter(YearMonth period, ObservableList<AttendanceLog> attendanceLogs) {
        this.period = period;
        this.attendanceLogs = attendanceLogs;
    }

    @Override
    public String getSheetName() {
        return "Detailed Payroll";
    }

    @Override
    public List<String> getHeaders() {
        return new ArrayList<>(); // Handled in writeDataToWorkbook due to multi-row headers
    }

    @Override
    public List<String> getRowData(Student student) {
        return new ArrayList<>(); // Handled in writeDataToWorkbook due to complex row structure
    }

    @Override
    public void writeDataToWorkbook(Workbook workbook, ObservableList<Student> items, String title) {
        Sheet sheet = workbook.createSheet(getSheetName());

        // Styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle centerStyle = createCenterStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle borderStyle = createBorderStyle(workbook);

        // Header Section (Rows 1-3)
        Row row1 = sheet.createRow(0);
        row1.createCell(0).setCellValue("GENERAL FORM NO. 7(A)");

        Row row2 = sheet.createRow(1);
        Cell titleCell = row2.createCell(3);
        titleCell.setCellValue("TIME BOOK AND PAYROLL");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 3, 34));

        Row row3 = sheet.createRow(2);
        Cell locationCell = row3.createCell(3);
        locationCell.setCellValue(
                "For labor on Baybay Data Center at Baybay Nat'l High School, Baybay City, Leyte, Philippines, for the period");
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 3, 31));
        Cell periodCell = row3.createCell(32);
        periodCell.setCellValue(period.getMonth().toString().toUpperCase() + " " + period.getYear());
        periodCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 32, 34));

        // Table Headers (Rows 5-7)
        createTableHeaders(sheet, headerStyle, centerStyle);

        // Data Rows (Starting Row 8)
        int rowIndex = 7;
        double grandTotal = 0;
        for (Student student : items) {
            Row dataRow = sheet.createRow(rowIndex++);
            grandTotal += fillDataRow(dataRow, student, centerStyle, currencyStyle, borderStyle);
        }

        // Sub-Total
        Row subTotalRow = sheet.createRow(rowIndex + 1);
        Cell subTotalLabel = subTotalRow.createCell(35);
        subTotalLabel.setCellValue("SUB-TOTAL FOR THIS PAGE " + PESO + String.format("%,.2f", grandTotal));
        subTotalLabel.setCellStyle(currencyStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, 35, 36));

        // Footer Section
        createFooterSection(sheet, rowIndex + 3);

        // Apply borders to table
        applyBorders(sheet, 4, rowIndex - 1, 0, 36);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createCenterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("₱#,##0.00"));
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void createTableHeaders(Sheet sheet, CellStyle headerStyle, CellStyle centerStyle) {
        Row row5 = sheet.createRow(4);
        Row row6 = sheet.createRow(5);
        Row row7 = sheet.createRow(6);

        // Main headers (Row 5)
        String[] mainHeaders = { "No.", "Name", "Week 1", "Week 2", "Week 3", "Week 4", "Week 5",
                "Total No. of Days Worked", "Transportation Allowance", "Meal Allowance",
                "Total Amount Due", "No.", "Signature" };
        int[] colStarts = { 0, 1, 2, 7, 13, 19, 25, 27, 29, 32, 35, 36 };
        int[] colEnds = { 0, 1, 6, 12, 18, 24, 26, 28, 31, 34, 35, 36 };
        for (int i = 0; i < mainHeaders.length; i++) {
            Cell cell = row5.createCell(colStarts[i]);
            cell.setCellValue(mainHeaders[i]);
            cell.setCellStyle(headerStyle);
            if (colStarts[i] != colEnds[i]) {
                sheet.addMergedRegion(new CellRangeAddress(4, 4, colStarts[i], colEnds[i]));
            }
        }

        // Sub-headers (Row 6)
        String[] weekDays = { "Day", "Th", "F", "M", "T", "W", "Th", "F", "M", "T", "W", "Th", "F", "M", "T", "W", "Th",
                "F", "M", "T", "W", "Th", "F" };
        int col = 2;
        for (String day : weekDays) {
            Cell cell = row6.createCell(col++);
            cell.setCellValue(day);
            cell.setCellStyle(centerStyle);
        }
        row6.createCell(27).setCellValue("Daily Rate").setCellStyle(centerStyle);
        row6.createCell(28).setCellValue("Amount Due").setCellStyle(centerStyle);
        sheet.addMergedRegion(new CellRangeAddress(5, 5, 28, 29));
        row6.createCell(30).setCellValue("Daily Rate").setCellStyle(centerStyle);
        row6.createCell(31).setCellValue("Amount").setCellStyle(centerStyle);
        sheet.addMergedRegion(new CellRangeAddress(5, 5, 31, 32));

        // Dates (Row 7) - Dynamic based on period
        col = 2;
        for (int day = 1; day <= period.lengthOfMonth() && col <= 26; day++) {
            LocalDate date = period.atDay(day);
            if (!AttendanceUtil.isWeekend(date)) {
                Cell cell = row7.createCell(col++);
                cell.setCellValue(day);
                cell.setCellStyle(centerStyle);
            }
        }
    }

    private double fillDataRow(Row row, Student student, CellStyle centerStyle, CellStyle currencyStyle,
            CellStyle borderStyle) {
        // Serial Number and Name
        row.createCell(0).setCellValue(student.getStudentID()).setCellStyle(centerStyle);
        String fullName = student.getLastName() + ", " + student.getFirstName() +
                (student.getMiddleName() != null ? " " + student.getMiddleName() : "") +
                (student.getNameExtension() != null ? " " + student.getNameExtension() : "");
        row.createCell(1).setCellValue(fullName).setCellStyle(borderStyle);

        // Attendance
        int totalDays = 0;
        int col = 2;
        for (int day = 1; day <= period.lengthOfMonth() && col <= 26; day++) {
            LocalDate date = period.atDay(day);
            if (!AttendanceUtil.isWeekend(date)) {
                String status = AttendanceUtil.getAttendanceStatus(student, date, attendanceLogs);
                if (status.equals(AttendanceUtil.PRESENT_MARK)) {
                    row.createCell(col).setCellValue(CHECKMARK).setCellStyle(centerStyle);
                    totalDays++;
                }
                col++;
            }
        }

        // Total Days Worked
        row.createCell(27).setCellValue(totalDays).setCellStyle(centerStyle);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 27, 28));

        // Transportation Allowance
        double transAmount = student.getFare() * totalDays;
        row.createCell(29).setCellValue(student.getFare()).setCellStyle(currencyStyle);
        row.createCell(30).setCellValue(transAmount).setCellStyle(currencyStyle);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 30, 31));

        // Meal Allowance (fixed rate)
        double mealDailyRate = 1300.00; // Adjust if needed
        double mealAmount = mealDailyRate * totalDays;
        row.createCell(32).setCellValue(mealDailyRate).setCellStyle(currencyStyle);
        row.createCell(33).setCellValue(mealAmount).setCellStyle(currencyStyle);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 33, 34));

        // Total Amount Due
        double totalAmount = transAmount + mealAmount;
        row.createCell(35).setCellValue(totalAmount).setCellStyle(currencyStyle);

        // Serial Number (repeated) and Signature
        row.createCell(36).setCellValue(student.getStudentID()).setCellStyle(centerStyle);
        row.createCell(37).setCellStyle(borderStyle); // Signature empty

        return totalAmount;
    }

    private void createFooterSection(Sheet sheet, int startRow) {
        Row certRow1 = sheet.createRow(startRow);
        certRow1.createCell(0).setCellValue(
                "1. I HEREBY CERTIFY on my official oath to the correctness of the above roll. Payment is hereby approved from the appropriation indicated.");
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, 6));
        certRow1.createCell(8).setCellValue("2. APPROVED");
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 8, 9));
        certRow1.createCell(11).setCellValue(
                "3. I HEREBY CERTIFY on my official oath that I have this ___ day of ____ paid in cash to each man whose name appears on the above roll, the amount set opposite his name, he having presented himself, established his identity and affixed his signature or thumbmark on the space provided therefor.");
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 11, 31));

        Row sigRow = sheet.createRow(startRow + 3);
        sigRow.createCell(0).setCellValue("JESCYN KATE N. RAMOS, E2P - ICT Coordinator");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, 0, 6));
        sigRow.createCell(8).setCellValue("CARLOS JERICHO L. PETILLA, Provincial Governor");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, 8, 9));
        sigRow.createCell(11).setCellValue("JOAN FLORLYN JUSTISA, E2P - ICT");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, 11, 31));

        Row noteRow = sheet.createRow(startRow + 5);
        noteRow.createCell(0).setCellValue(
                "NOTE: Where thumbmark is to be used in place of signature, and the space available is not sufficient, the thumbmark may be impressed on the back hereof with proper indication of the corresponding student's name and on the corresponding line on the payroll or remark 'see thumbmark on the back' should be written.");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 5, startRow + 5, 0, 31));

        Row taglineRow = sheet.createRow(startRow + 6);
        taglineRow.createCell(0).setCellValue("‘IPAKITA SA MUNDO, UMAASENSA NA TAYO’");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 6, startRow + 6, 0, 31));
    }

    private void applyBorders(Sheet sheet, int startRow, int endRow, int startCol, int endCol) {
        for (int r = startRow; r <= endRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null)
                row = sheet.createRow(r);
            for (int c = startCol; c <= endCol; c++) {
                Cell cell = row.getCell(c);
                if (cell == null)
                    cell = row.createCell(c);
                CellStyle style = cell.getCellStyle();
                if (style == null)
                    style = sheet.getWorkbook().createCellStyle();
                style.setBorderTop(BorderStyle.THIN);
                style.setBorderBottom(BorderStyle.THIN);
                style.setBorderLeft(BorderStyle.THIN);
                style.setBorderRight(BorderStyle.THIN);
                cell.setCellStyle(style);
            }
        }
    }

    @Override
    public void writeDataToPdf(Document document, ObservableList<Student> items, String title) {
        // Implement PDF export if needed (e.g., using iText)
    }

    @Override
    public void writeDataToCsv(PrintWriter writer, ObservableList<Student> items, String title) {
        // Implement CSV export if needed
    }
}