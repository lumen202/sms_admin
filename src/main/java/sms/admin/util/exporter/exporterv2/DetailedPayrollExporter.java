package sms.admin.util.exporter.exporterv2;

// Keep only necessary imports for Excel
import javafx.collections.ObservableList;
import sms.admin.util.attendance.AttendanceUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import java.time.LocalDate;
import java.time.YearMonth;

public class DetailedPayrollExporter extends PayrollExcelExporter {

    private static final String CHECKMARK = "\u2713"; // ✓
    private final YearMonth period;
    private final ObservableList<AttendanceLog> attendanceLogs;

    public DetailedPayrollExporter(YearMonth period, ObservableList<AttendanceLog> attendanceLogs) {
        this.period = period;
        this.attendanceLogs = attendanceLogs;
    }

    @Override
    public String getSheetName() {
        return "Detailed Payroll";
    }

    @Override
    public void writeDataToWorkbook(Workbook workbook, ObservableList<Student> items, String title) {
        Sheet sheet = workbook.createSheet(getSheetName());

        // Styles
        CellStyle headerStyle = super.createHeaderStyle(workbook);
        CellStyle centerStyle = super.createCenterStyle(workbook);
        CellStyle currencyStyle = super.createCurrencyStyle(workbook);
        CellStyle borderStyle = super.createBorderStyle(workbook);

        // Header Section (Rows 1-3)
        Row row1 = sheet.createRow(0);
        Cell formCell = row1.createCell(0);
        formCell.setCellValue("GENERAL FORM NO. 7(A)");

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
            grandTotal += fillDataRow(sheet, dataRow, student, centerStyle, currencyStyle, borderStyle);
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
        super.applyBorders(sheet, 4, rowIndex - 1, 0, 36);
    }

    private void createTableHeaders(Sheet sheet, CellStyle headerStyle, CellStyle centerStyle) {
        Row row5 = sheet.createRow(4);
        Row row6 = sheet.createRow(5);
        Row row7 = sheet.createRow(6);

        // Main headers (Row 5)
        String[] mainHeaders = { 
            "No.", "Name", "Week 1", "Week 2", "Week 3", "Week 4", "Week 5",
            "Total No. of Days Worked", "Transportation Allowance", "Meal Allowance",
            "Total Amount Due", "No.", "Signature" 
        };
        int[] colStarts = { 0, 1, 2, 7, 13, 19, 25, 27, 29, 32, 35, 36, 37 }; // Added extra column
        int[] colEnds = { 0, 1, 6, 12, 18, 24, 26, 28, 31, 34, 35, 36, 37 }; // Added extra column

        // Safety check for array lengths
        int minLength = Math.min(Math.min(mainHeaders.length, colStarts.length), colEnds.length);
        
        for (int i = 0; i < minLength; i++) {
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
        Cell dailyRateCell1 = row6.createCell(27);
        dailyRateCell1.setCellValue("Daily Rate");
        dailyRateCell1.setCellStyle(centerStyle);
        Cell amountDueCell1 = row6.createCell(28);
        amountDueCell1.setCellValue("Amount Due");
        amountDueCell1.setCellStyle(centerStyle);
        sheet.addMergedRegion(new CellRangeAddress(5, 5, 28, 29));
        Cell dailyRateCell2 = row6.createCell(30);
        dailyRateCell2.setCellValue("Daily Rate");
        dailyRateCell2.setCellStyle(centerStyle);
        Cell amountCell = row6.createCell(31);
        amountCell.setCellValue("Amount");
        amountCell.setCellStyle(centerStyle);
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

    private double fillDataRow(Sheet sheet, Row row, Student student, CellStyle centerStyle, CellStyle currencyStyle,
            CellStyle borderStyle) {
        // Serial Number and Name
        Cell serialCell = row.createCell(0);
        serialCell.setCellValue(student.getStudentID());
        serialCell.setCellStyle(centerStyle);

        String fullName = student.getLastName() + ", " + student.getFirstName() +
                (student.getMiddleName() != null ? " " + student.getMiddleName() : "") +
                (student.getNameExtension() != null ? " " + student.getNameExtension() : "");
        Cell nameCell = row.createCell(1);
        nameCell.setCellValue(fullName);
        nameCell.setCellStyle(borderStyle);

        // Attendance
        int totalDays = 0;
        int col = 2;
        for (int day = 1; day <= period.lengthOfMonth() && col <= 26; day++) {
            LocalDate date = period.atDay(day);
            if (!AttendanceUtil.isWeekend(date)) {
                Cell attendanceCell = row.createCell(col);
                String status = AttendanceUtil.getAttendanceStatus(student, date, attendanceLogs);
                if (status.equals(AttendanceUtil.PRESENT_MARK)) {
                    attendanceCell.setCellValue(CHECKMARK);
                    attendanceCell.setCellStyle(centerStyle);
                    totalDays++;
                } else {
                    attendanceCell.setCellValue("");
                    attendanceCell.setCellStyle(centerStyle);
                }
                col++;
            }
        }

        // Total Days Worked
        Cell totalDaysCell = row.createCell(27);
        totalDaysCell.setCellValue(totalDays);
        totalDaysCell.setCellStyle(centerStyle);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 27, 28));

        // Transportation Allowance
        double transAmount = student.getFare() * totalDays;
        Cell fareCell = row.createCell(29);
        fareCell.setCellValue(student.getFare());
        fareCell.setCellStyle(currencyStyle);
        Cell transAmountCell = row.createCell(30);
        transAmountCell.setCellValue(transAmount);
        transAmountCell.setCellStyle(currencyStyle);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 30, 31));

        // Meal Allowance (fixed rate)
        double mealDailyRate = 1300.00; // Adjust if needed
        double mealAmount = mealDailyRate * totalDays;
        Cell mealRateCell = row.createCell(32);
        mealRateCell.setCellValue(mealDailyRate);
        mealRateCell.setCellStyle(currencyStyle);
        Cell mealAmountCell = row.createCell(33);
        mealAmountCell.setCellValue(mealAmount);
        mealAmountCell.setCellStyle(currencyStyle);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 33, 34));

        // Total Amount Due
        double totalAmount = transAmount + mealAmount;
        Cell totalAmountCell = row.createCell(35);
        totalAmountCell.setCellValue(totalAmount);
        totalAmountCell.setCellStyle(currencyStyle);

        // Serial Number (repeated) and Signature
        Cell serialCellRepeated = row.createCell(36);
        serialCellRepeated.setCellValue(student.getStudentID());
        serialCellRepeated.setCellStyle(centerStyle);
        Cell signatureCell = row.createCell(37);
        signatureCell.setCellStyle(borderStyle); // Signature empty

        return totalAmount;
    }

    private void createFooterSection(Sheet sheet, int startRow) {
        Row certRow1 = sheet.createRow(startRow);
        Cell cert1Cell = certRow1.createCell(0);
        cert1Cell.setCellValue(
                "1. I HEREBY CERTIFY on my official oath to the correctness of the above roll. Payment is hereby approved from the appropriation indicated.");
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, 6));

        Cell approvedCell = certRow1.createCell(8);
        approvedCell.setCellValue("2. APPROVED");
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 8, 9));

        Cell cert2Cell = certRow1.createCell(11);
        cert2Cell.setCellValue(
                "3. I HEREBY CERTIFY on my official oath that I have this ___ day of ____ paid in cash to each man whose name appears on the above roll, the amount set opposite his name, he having presented himself, established his identity and affixed his signature or thumbmark on the space provided therefor.");
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 11, 31));

        Row sigRow = sheet.createRow(startRow + 3);
        Cell sig1Cell = sigRow.createCell(0);
        sig1Cell.setCellValue("JESCYN KATE N. RAMOS, E2P - ICT Coordinator");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, 0, 6));
        Cell sig2Cell = sigRow.createCell(8);
        sig2Cell.setCellValue("CARLOS JERICHO L. PETILLA, Provincial Governor");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, 8, 9));
        Cell sig3Cell = sigRow.createCell(11);
        sig3Cell.setCellValue("JOAN FLORLYN JUSTISA, E2P - ICT");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, 11, 31));

        Row noteRow = sheet.createRow(startRow + 5);
        Cell noteCell = noteRow.createCell(0);
        noteCell.setCellValue(
                "NOTE: Where thumbmark is to be used in place of signature, and the space available is not sufficient, the thumbmark may be impressed on the back hereof with proper indication of the corresponding student's name and on the corresponding line on the payroll or remark 'see thumbmark on the back' should be written.");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 5, startRow + 5, 0, 31));

        Row taglineRow = sheet.createRow(startRow + 6);
        Cell taglineCell = taglineRow.createCell(0);
        taglineCell.setCellValue("‘IPAKITA SA MUNDO, UMAASENSA NA TAYO’");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 6, startRow + 6, 0, 31));
    }
}