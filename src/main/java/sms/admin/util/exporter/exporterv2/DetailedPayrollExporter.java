package sms.admin.util.exporter.exporterv2;

import javafx.collections.ObservableList;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import sms.admin.util.attendance.AttendanceUtil;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

public class DetailedPayrollExporter extends PayrollExcelExporter {

    // Constants for symbols
    private static final String CHECKMARK = "\u2713"; // ✓
    private static final String PESO = "P"; // Currency symbol
    private static final String ABSENT_MARK = "✗"; // X for absent

    // Instance variables
    private final YearMonth period;
    private final ObservableList<AttendanceLog> attendanceLogs;

    // Constructor
    public DetailedPayrollExporter(YearMonth period, ObservableList<AttendanceLog> attendanceLogs) {
        this.period = period;
        this.attendanceLogs = attendanceLogs;
    }

    // Override to set the sheet name
    @Override
    public String getSheetName() {
        return "Detailed Payroll";
    }

    // Main method to write data to the workbook
    @Override
    public void writeDataToWorkbook(Workbook workbook, ObservableList<Student> items, String title) {
        Sheet sheet = workbook.createSheet(getSheetName());

        // Adjust column widths
        sheet.setColumnWidth(0, 10 * 256); // No.
        sheet.setColumnWidth(1, 50 * 256); // Name (increased)
        // Week columns
        for (int i = 2; i <= 26; i++) {
            sheet.setColumnWidth(i, 8 * 256);
        }
        sheet.setColumnWidth(27, 12 * 256); // Total Days
        sheet.setColumnWidth(29, 15 * 256); // Transportation
        sheet.setColumnWidth(32, 15 * 256); // Meal
        sheet.setColumnWidth(35, 15 * 256); // Total Amount
        sheet.setColumnWidth(36, 10 * 256); // No.
        sheet.setColumnWidth(37, 20 * 256); // Signature

        // Styles with larger fonts
        CellStyle headerStyle = super.createHeaderStyle(workbook);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 14); // Increased font size
        headerStyle.setFont(headerFont);

        // Styles
        CellStyle centerStyle = super.createCenterStyle(workbook);
        CellStyle currencyStyle = super.createCurrencyStyle(workbook);
        CellStyle borderStyle = super.createBorderStyle(workbook);

        // Header Section (Rows 1-3)
        Row row0 = sheet.createRow(0);
        Cell formCell = row0.createCell(0);
        formCell.setCellValue("GENERAL FORM NO. 7(A)");
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 37));
        formCell.setCellStyle(centerStyle);

        Row row1 = sheet.createRow(1);
        Cell titleCell = row1.createCell(0);
        titleCell.setCellValue("TIME BOOK AND PAYROLL");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 37));

        Row row2 = sheet.createRow(2);
        Cell locationCell = row2.createCell(0);
        locationCell.setCellValue(
                "For labor on Baybay Data Center at Baybay Nat'l High School, Baybay City, Leyte, Philippines, for the period "
                        +
                        period.getMonth().toString().toUpperCase() + " " + period.getYear());
        locationCell.setCellStyle(centerStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 37));

        // Row 3 is left empty
        Row row3 = sheet.createRow(3);

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
        Cell subTotalLabel = subTotalRow.createCell(34);
        subTotalLabel.setCellValue("SUB - TOTAL FOR THIS PAGE " + PESO + String.format("%,.2f", grandTotal));
        subTotalLabel.setCellStyle(currencyStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, 34, 35));

        // Footer Section
        createFooterSection(sheet, rowIndex + 3);

        // Apply borders to the table area
        super.applyBorders(sheet, 4, rowIndex - 1, 0, 37);
    }

    // Helper Method: Create Table Headers
    private void createTableHeaders(Sheet sheet, CellStyle headerStyle, CellStyle centerStyle) {
        // Create header rows
        Row row4 = sheet.createRow(4); // TIME ROLL
        Row row5 = sheet.createRow(5); // Week numbers
        Row row6 = sheet.createRow(6); // Day names
        Row row7 = sheet.createRow(7); // Dates

        // Create a non-bold style for headers
        CellStyle normalHeaderStyle = sheet.getWorkbook().createCellStyle();
        normalHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        normalHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        normalHeaderStyle.setWrapText(true); // Allow text wrapping

        // Get weeks data
        List<List<LocalDate>> weeks = getWorkWeeks();
        int totalColumns = weeks.stream().mapToInt(List::size).sum();

        // No. and Name headers (don't merge single cells)
        Cell noCell = row4.createCell(0);
        noCell.setCellValue("No.");
        noCell.setCellStyle(normalHeaderStyle);

        Cell nameCell = row4.createCell(1);
        nameCell.setCellValue("Student Name");
        nameCell.setCellStyle(normalHeaderStyle);

        // TIME ROLL header with merged rows if necessary
        Cell timeRollCell = row4.createCell(2);
        timeRollCell.setCellValue("TIME ROLL");
        timeRollCell.setCellStyle(normalHeaderStyle);
        if (totalColumns > 1) {
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 2, 2 + totalColumns - 1));
        }

        // Create empty cells for merging
        for (int r = 5; r <= 7; r++) {
            Row emptyRow = sheet.getRow(r);
            if (emptyRow == null) {
                emptyRow = sheet.createRow(r);
            }
            for (int c = 0; c <= 1; c++) {
                Cell emptyCell = emptyRow.createCell(c);
                emptyCell.setCellStyle(normalHeaderStyle);
            }
        }

        // Add week headers and merge with days
        int currentCol = 2;
        for (int weekNum = 0; weekNum < weeks.size(); weekNum++) {
            List<LocalDate> week = weeks.get(weekNum);
            if (!week.isEmpty()) {
                // Week header
                Cell weekCell = row5.createCell(currentCol);
                weekCell.setCellValue("Week " + (weekNum + 1));
                weekCell.setCellStyle(normalHeaderStyle);
                
                if (week.size() > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(5, 5, currentCol, currentCol + week.size() - 1));
                }

                // Days and dates
                for (LocalDate date : week) {
                    Cell dayCell = row6.createCell(currentCol);
                    dayCell.setCellValue(date.getDayOfWeek().toString().substring(0, 2));
                    dayCell.setCellStyle(centerStyle);
                    
                    Cell dateCell = row7.createCell(currentCol);
                    dateCell.setCellValue(date.getDayOfMonth());
                    dateCell.setCellStyle(centerStyle);
                    
                    currentCol++;
                }
            }
        }

        // Total Days as a single column
        Cell totalDaysCell = row4.createCell(currentCol);
        totalDaysCell.setCellValue("Total No. of Days");
        totalDaysCell.setCellStyle(normalHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(4, 7, currentCol, currentCol));
        currentCol++;

        // Rest of the headers with proper merging
        addRemainingHeaders(sheet, headerStyle, currentCol);
    }

    private void addRemainingHeaders(Sheet sheet, CellStyle headerStyle, int startCol) {
        // Transportation Allowance header (2 columns)
        Cell transCell = sheet.getRow(4).createCell(startCol);
        transCell.setCellValue("Transportation Allowance");
        transCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(4, 4, startCol, startCol + 1));
        
        // Add "Daily Rate" and "Amount Due" sub-headers
        Cell rateCell = sheet.getRow(5).createCell(startCol);
        rateCell.setCellValue("Daily Rate");
        rateCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(5, 7, startCol, startCol));
        
        Cell amountCell = sheet.getRow(5).createCell(startCol + 1);
        amountCell.setCellValue("Amount Due");
        amountCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(5, 7, startCol + 1, startCol + 1));

        // ... similar pattern for Meal Allowance and other headers ...
    }

    private List<List<LocalDate>> getWorkWeeks() {
        List<List<LocalDate>> weeks = new ArrayList<>();
        List<LocalDate> currentWeek = new ArrayList<>();
        LocalDate firstDay = period.atDay(1);

        // Initialize first week
        int currentWeekNum = firstDay.get(WeekFields.ISO.weekOfWeekBasedYear());
        int weekCounter = 0;

        for (int day = 1; day <= period.lengthOfMonth(); day++) {
            LocalDate date = period.atDay(day);

            // Skip weekends
            if (AttendanceUtil.isWeekend(date)) {
                continue;
            }

            int weekNum = date.get(WeekFields.ISO.weekOfWeekBasedYear());

            // If new week starts and has workdays, save current week
            if (weekNum != currentWeekNum) {
                if (!currentWeek.isEmpty()) {
                    weeks.add(new ArrayList<>(currentWeek));
                    weekCounter++;
                    currentWeek.clear();
                }
                currentWeekNum = weekNum;
            }

            currentWeek.add(date);
        }

        // Add last week if it has workdays
        if (!currentWeek.isEmpty()) {
            weeks.add(currentWeek);
            weekCounter++;
        }

        return weeks;
    }

    // Helper Method: Fill Data Row
    private double fillDataRow(Sheet sheet, Row row, Student student, CellStyle centerStyle, CellStyle currencyStyle,
            CellStyle borderStyle) {
        // Student ID
        Cell serialCell = row.createCell(0);
        serialCell.setCellValue(student.getStudentID());
        serialCell.setCellStyle(centerStyle);

        // Full Name
        String fullName = student.getLastName().toUpperCase() + ", " + student.getFirstName() +
                (student.getMiddleName() != null ? " " + student.getMiddleName().toUpperCase() : "") +
                (student.getNameExtension() != null ? " " + student.getNameExtension() : "");
        Cell nameCell = row.createCell(1);
        nameCell.setCellValue(fullName);
        nameCell.setCellStyle(borderStyle);

        // Get work weeks
        List<List<LocalDate>> weeks = getWorkWeeks();
        int currentCol = 2;

        // Process each week that has workdays
        int totalDays = 0;
        for (List<LocalDate> week : weeks) {
            for (LocalDate date : week) {
                Cell attendanceCell = row.createCell(currentCol++);
                String status = AttendanceUtil.getAttendanceStatus(student, date, attendanceLogs);
                if (status.equals(AttendanceUtil.PRESENT_MARK)) {
                    attendanceCell.setCellValue(CHECKMARK);
                    totalDays++;
                } else {
                    attendanceCell.setCellValue(ABSENT_MARK);
                }
                attendanceCell.setCellStyle(centerStyle);
            }
        }

        // Calculate next column position
        currentCol = 2 + weeks.stream().mapToInt(List::size).sum();

        // Total Days Worked (single column)
        Cell totalDaysCell = row.createCell(currentCol++);
        totalDaysCell.setCellValue(totalDays);
        totalDaysCell.setCellStyle(centerStyle);

        // Transportation Allowance
        double transAmount = student.getFare() * totalDays;
        Cell fareCell = row.createCell(currentCol++);
        fareCell.setCellValue(student.getFare());
        fareCell.setCellStyle(currencyStyle);
        Cell transAmountCell = row.createCell(currentCol++);
        transAmountCell.setCellValue(transAmount);
        transAmountCell.setCellStyle(currencyStyle);

        // Meal Allowance
        double mealDailyRate = 1300.00;
        double mealAmount = mealDailyRate * totalDays;
        Cell mealRateCell = row.createCell(currentCol++);
        mealRateCell.setCellValue(mealDailyRate);
        mealRateCell.setCellStyle(currencyStyle);
        Cell mealAmountCell = row.createCell(currentCol++);
        mealAmountCell.setCellValue(mealAmount);
        mealAmountCell.setCellStyle(currencyStyle);

        // Total Amount Received (no merging)
        double totalAmount = transAmount + mealAmount;
        Cell totalAmountCell = row.createCell(currentCol++);
        totalAmountCell.setCellValue(totalAmount);
        totalAmountCell.setCellStyle(currencyStyle);

        // Repeated Student ID and Signature
        Cell serialCellRepeated = row.createCell(currentCol++);
        serialCellRepeated.setCellValue(student.getStudentID());
        serialCellRepeated.setCellStyle(centerStyle);
        Cell signatureCell = row.createCell(currentCol);
        signatureCell.setCellStyle(borderStyle);

        return totalAmount;
    }

    // Helper Method: Create Footer Section
    private void createFooterSection(Sheet sheet, int startRow) {
        // Certification Row 1
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

        // Signature Row
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

        // Note Row
        Row noteRow = sheet.createRow(startRow + 5);
        Cell noteCell = noteRow.createCell(0);
        noteCell.setCellValue(
                "NOTE: Where thumbmark is to be used in place of signature, and the space available is not sufficient, the thumbmark may be impressed on the back hereof with proper indication of the corresponding student's name and on the corresponding line on the payroll or remark 'see thumbmark on the back' should be written.");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 5, startRow + 5, 0, 31));

        // Tagline Row
        Row taglineRow = sheet.createRow(startRow + 6);
        Cell taglineCell = taglineRow.createCell(0);
        taglineCell.setCellValue("‘IPAKITA SA MUNDO, UMAASENSA NA TAYO’");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 6, startRow + 6, 0, 31));
    }
}