package sms.admin.util.exporter.exporterv2;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import sms.admin.util.attendance.CommonAttendanceUtil;

public class DetailedPayrollExporter {
    // Constants
    private static final String CHECKMARK = "✓";
    private static final String PESO = "₱";
    private static final String ABSENT_MARK = "✗";
    private static final double MEAL_DAILY_RATE = 1300.00;
    private static final double MAX_TOTAL_AMOUNT = 1300.00;

    // Instance variables
    private final YearMonth month;
    private final ObservableList<AttendanceLog> attendanceLogs;
    private double fareMultiplier = 1.0;
    private int totalDaysColumn;
    private int totalAmountColumn;

    // Constructor and public methods
    public DetailedPayrollExporter(YearMonth month, YearMonth endMonth, ObservableList<AttendanceLog> logs) {
        this.month = month;
        this.attendanceLogs = logs;
    }

    public void setFareMultiplier(double multiplier) {
        this.fareMultiplier = multiplier;
    }

    // Main export method
    public void exportToExcel(TableView<Student> table, String title, String outputPath) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(title);

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);

            // Set improved column widths
            sheet.setColumnWidth(0, 12 * 256); // No. (wider for larger numbers)
            sheet.setColumnWidth(1, 50 * 256); // Name (extra wide for long names)
            // Days columns - slightly wider for better readability
            for (int i = 2; i <= 26; i++) {
                sheet.setColumnWidth(i, 7 * 256);
            }
            sheet.setColumnWidth(32, 25 * 256); // Total Amount column (25 units)
            sheet.setColumnWidth(33, 12 * 256); // No.
            sheet.setColumnWidth(34, 30 * 256); // Signature (extra wide for writing space)

            // Increase row heights for better readability
            short normalRowHeight = (short) (25 * 20);
            short headerRowHeight = (short) (30 * 20);
            short titleRowHeight = (short) (35 * 20);

            // Form number - centered and bold with increased height
            Row formRow = sheet.createRow(0);
            formRow.setHeight(headerRowHeight);
            Cell formCell = formRow.createCell(0);
            formCell.setCellValue("GENERAL FORM NO. 7(A)");
            CellStyle formStyle = createHeaderStyle(workbook);
            formStyle.setAlignment(HorizontalAlignment.CENTER);
            formCell.setCellStyle(formStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 34));

            // Title - larger font and bold with maximum height
            Row titleRow = sheet.createRow(1);
            titleRow.setHeight(titleRowHeight);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("TIME BOOK AND PAYROLL");
            CellStyle titleStyle = createHeaderStyle(workbook);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 34));

            // Period description with increased height
            Row periodRow = sheet.createRow(2);
            periodRow.setHeight(headerRowHeight);
            Cell periodCell = periodRow.createCell(0);

            // Create custom period style
            CellStyle periodStyle = workbook.createCellStyle();
            Font periodFont = workbook.createFont();
            periodFont.setBold(true);
            periodFont.setFontHeightInPoints((short) 14); // Larger font size
            periodStyle.setFont(periodFont);
            periodStyle.setAlignment(HorizontalAlignment.CENTER);
            periodStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            periodCell.setCellValue(
                    "For labor on Baybay Data Center at Baybay National High School, Baybay City, Leyte, Philippines, for the period,    "
                            + month.getMonth().toString() + " " + month.getYear());
            periodCell.setCellStyle(periodStyle); // Use new style
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 34));

            // Empty row with smaller height for spacing
            sheet.createRow(3).setHeight((short) (15 * 20));

            // Get weeks and calculate total days
            List<List<LocalDate>> weeks = getWorkWeeks();
            int totalDays = weeks.stream().mapToInt(List::size).sum();

            // Store column indices for later use
            totalDaysColumn = 2 + totalDays;
            totalAmountColumn = totalDaysColumn + 5; // After Total Days + Trans Rate/Amount + Meal Rate/Amount

            // Get total days from header creation
            totalDays = createTableHeaders(sheet, 4, headerStyle, centerStyle, weeks);
            int totalAmountCol = 2 + totalDays + 5; // 2 (No./Name) + totalDays + 5 (headers after days)

            // Write data rows
            int rowNum = 8;
            double grandTotal = 0;
            for (Student student : table.getItems()) {
                Row row = sheet.createRow(rowNum++);
                row.setHeight(normalRowHeight);
                grandTotal += writeStudentRow(row, student, weeks, dataStyle, currencyStyle, centerStyle);
            }

            // Create subtotal row immediately after data (no extra space)
            Row subtotalRow = sheet.createRow(rowNum++);
            subtotalRow.setHeight(normalRowHeight);

            // Create and merge subtotal cells
            Cell subtotalLabel = subtotalRow.createCell(0);
            subtotalLabel.setCellValue("SUB - TOTAL FOR THIS PAGE:  ");
            subtotalLabel.setCellStyle(totalStyle);
            sheet.addMergedRegion(new CellRangeAddress(
                    subtotalRow.getRowNum(),
                    subtotalRow.getRowNum(),
                    0,
                    totalAmountCol - 1));

            Cell subtotalAmount = subtotalRow.createCell(totalAmountCol);
            subtotalAmount.setCellValue(grandTotal);
            subtotalAmount.setCellStyle(currencyStyle);

            // Add certification section with proper spacing
            rowNum += 2;
            createCertificationSection(sheet, rowNum);

            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            }
        }
    }

    // Data processing methods
    private List<List<LocalDate>> getWorkWeeks() {
        List<List<LocalDate>> weeks = new ArrayList<>();
        List<LocalDate> currentWeek = new ArrayList<>();
        LocalDate firstDay = month.atDay(1);
        int currentWeekNum = firstDay.get(WeekFields.ISO.weekOfWeekBasedYear());

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            if (CommonAttendanceUtil.isWeekend(date)) {
                continue;
            }

            int weekNum = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            if (weekNum != currentWeekNum && !currentWeek.isEmpty()) {
                weeks.add(new ArrayList<>(currentWeek));
                currentWeek.clear();
                currentWeekNum = weekNum;
            }
            currentWeek.add(date);
        }

        if (!currentWeek.isEmpty()) {
            weeks.add(currentWeek);
        }

        return weeks;
    }

    private boolean isPresent(Student student, LocalDate date) {
        return attendanceLogs.stream()
                .filter(log -> log != null
                        && log.getStudentID() != null
                        && log.getStudentID().getStudentID() == student.getStudentID()
                        && log.getRecordID() != null
                        && log.getRecordID().getYear() == date.getYear()
                        && log.getRecordID().getMonth() == date.getMonthValue()
                        && log.getRecordID().getDay() == date.getDayOfMonth())
                .anyMatch(log -> {
                    String status = CommonAttendanceUtil.computeAttendanceStatus(log);
                    return status.equals(CommonAttendanceUtil.PRESENT_MARK)
                            || status.equals(CommonAttendanceUtil.EXCUSED_MARK)
                            || status.equals(CommonAttendanceUtil.HOLIDAY_MARK);
                });
    }

    private String formatFullName(Student student) {
        return String.format("%s, %s %s%s",
                student.getLastName().toUpperCase(),
                student.getFirstName(),
                student.getMiddleName() != null ? student.getMiddleName().toUpperCase() : "",
                student.getNameExtension() != null ? " " + student.getNameExtension() : "");
    }

    private String formatDayName(LocalDate date) {
        return CommonAttendanceUtil.getDayInitial(date.getDayOfWeek());
    }

    // Excel sheet building methods
    private int createTableHeaders(Sheet sheet, int startRow, CellStyle headerStyle,
            CellStyle centerStyle, List<List<LocalDate>> weeks) {
        // Create rows with consistent height
        Row headerRow1 = sheet.createRow(startRow);
        Row headerRow2 = sheet.createRow(startRow + 1);
        Row headerRow3 = sheet.createRow(startRow + 2);
        Row headerRow4 = sheet.createRow(startRow + 3);

        // Set consistent row heights
        headerRow1.setHeight((short) (25 * 20));
        headerRow2.setHeight((short) (25 * 20));
        headerRow3.setHeight((short) (25 * 20));
        headerRow4.setHeight((short) (25 * 20));

        // Basic columns
        Cell noHeader = headerRow1.createCell(0);
        noHeader.setCellValue("No.");
        noHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, 0, 0));

        Cell nameHeader = headerRow1.createCell(1);
        nameHeader.setCellValue("Student Name");
        nameHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, 1, 1));

        // Time roll section
        int currentCol = 2;
        Cell timeRollHeader = headerRow1.createCell(currentCol);
        timeRollHeader.setCellValue("TIME ROLL");
        timeRollHeader.setCellStyle(headerStyle);

        int totalDays = weeks.stream().mapToInt(List::size).sum();
        if (totalDays > 0) {
            sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, currentCol, currentCol + totalDays - 1));
        }

        // Week headers with day names instead of month names
        for (int weekNum = 0; weekNum < weeks.size(); weekNum++) {
            List<LocalDate> week = weeks.get(weekNum);
            if (!week.isEmpty()) {
                Cell weekHeader = headerRow2.createCell(currentCol);
                weekHeader.setCellValue("Week " + (weekNum + 1));
                weekHeader.setCellStyle(centerStyle);
                if (week.size() > 1) {
                    sheet.addMergedRegion(
                            new CellRangeAddress(startRow + 1, startRow + 1, currentCol, currentCol + week.size() - 1));
                }

                for (LocalDate date : week) {
                    // Add the day name in third row and date in fourth row
                    Cell dayNameCell = headerRow3.createCell(currentCol);
                    dayNameCell.setCellValue(formatDayName(date)); // Use new format
                    dayNameCell.setCellStyle(centerStyle);

                    Cell dayCell = headerRow4.createCell(currentCol);
                    dayCell.setCellValue(date.getDayOfMonth());
                    dayCell.setCellStyle(centerStyle);

                    currentCol++;
                }
            }
        }

        // Rest of the headers with improved alignment
        currentCol = 2 + totalDays;

        // Add remaining headers (Total Days, Allowances, etc.)
        addRemainingHeaders(sheet, startRow, currentCol, headerStyle, centerStyle);

        return totalDays; // Return total days
    }

    private void addRemainingHeaders(Sheet sheet, int startRow, int startCol,
            CellStyle headerStyle, CellStyle centerStyle) {
        CellStyle subHeaderStyle = createSubHeaderStyle(sheet.getWorkbook());
        int currentCol = startCol;

        // Total Days
        Cell totalDaysHeader = sheet.getRow(startRow).createCell(currentCol);
        totalDaysHeader.setCellValue("Total Days");
        totalDaysHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, currentCol, currentCol));
        currentCol++;
        // Transportation Allowance
        Cell transHeader = sheet.getRow(startRow).createCell(currentCol);
        transHeader.setCellValue("Transportation Allowance");
        transHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, currentCol, currentCol + 1));

        // Set sub-columns to sum to 25 (same as Total Amount)
        sheet.setColumnWidth(currentCol, 12 * 256); // Daily Rate (12 units)
        sheet.setColumnWidth(currentCol + 1, 13 * 256); // Total Amount (13 units)

        // Transportation sub-headers
        Cell transRateHeader = sheet.getRow(startRow + 1).createCell(currentCol);
        transRateHeader.setCellValue("Daily Rate");
        transRateHeader.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 3, currentCol, currentCol));

        Cell transAmountHeader = sheet.getRow(startRow + 1).createCell(currentCol + 1);
        transAmountHeader.setCellValue("Amount Due");
        transAmountHeader.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 3, currentCol + 1, currentCol + 1));
        currentCol += 2;

        // Meal Allowance
        Cell mealHeader = sheet.getRow(startRow).createCell(currentCol);
        mealHeader.setCellValue("Meal Allowance");
        mealHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, currentCol, currentCol + 1));

        // Set sub-columns to sum to 25 (same as Total Amount)
        sheet.setColumnWidth(currentCol, 12 * 256); // Daily Rate (12 units)
        sheet.setColumnWidth(currentCol + 1, 13 * 256); // Total Amount (13 units)

        // Meal sub-headers
        Cell mealRateHeader = sheet.getRow(startRow + 1).createCell(currentCol);
        mealRateHeader.setCellValue("Daily Rate");
        mealRateHeader.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 3, currentCol, currentCol));

        Cell mealAmountHeader = sheet.getRow(startRow + 1).createCell(currentCol + 1);
        mealAmountHeader.setCellValue("Amount Due");
        mealAmountHeader.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 3, currentCol + 1, currentCol + 1));
        currentCol += 2;

        // Total Amount (using stored column index)
        Cell totalAmountHeader = sheet.getRow(startRow).createCell(totalAmountColumn);
        totalAmountHeader.setCellValue("Total Amount Received");
        totalAmountHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, totalAmountColumn, totalAmountColumn));
        currentCol = totalAmountColumn + 1;

        // No. for signature
        Cell signNoHeader = sheet.getRow(startRow).createCell(currentCol);
        signNoHeader.setCellValue("No.");
        signNoHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, currentCol, currentCol));
        currentCol++;

        // Signature
        Cell signatureHeader = sheet.getRow(startRow).createCell(currentCol);
        signatureHeader.setCellValue("Signature");
        signatureHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, currentCol, currentCol));
    }

    private double writeStudentRow(Row row, Student student, List<List<LocalDate>> weeks,
            CellStyle dataStyle, CellStyle currencyStyle, CellStyle centerStyle) {
        int colNum = 0;

        // Student number
        Cell idCell = row.createCell(colNum++);
        idCell.setCellValue(student.getStudentID());
        idCell.setCellStyle(centerStyle);

        // Student name
        Cell nameCell = row.createCell(colNum++);
        nameCell.setCellValue(formatFullName(student));
        nameCell.setCellStyle(centerStyle);

        // Attendance marks
        int totalDays = 0;
        for (List<LocalDate> week : weeks) {
            for (LocalDate date : week) {
                Cell attendanceCell = row.createCell(colNum++);
                if (isPresent(student, date)) {
                    attendanceCell.setCellValue(CHECKMARK);
                    totalDays++;
                } else {
                    attendanceCell.setCellValue(ABSENT_MARK);
                }
                attendanceCell.setCellStyle(centerStyle);
            }
        }

        // Total days
        Cell totalDaysCell = row.createCell(colNum++);
        totalDaysCell.setCellValue(totalDays);
        totalDaysCell.setCellStyle(centerStyle);

        // Transportation allowance
        double fareRate = student.getFare() * fareMultiplier;
        double fareAmount = fareRate * totalDays;

        // Daily Rate (Fare)
        Cell fareRateCell = row.createCell(colNum++);
        fareRateCell.setCellValue(fareRate);
        fareRateCell.setCellStyle(currencyStyle);

        // Amount Due (Total transportation)
        Cell fareAmountCell = row.createCell(colNum++);
        fareAmountCell.setCellValue(fareAmount);
        fareAmountCell.setCellStyle(currencyStyle);

        // Meal allowance - leave blank as requested
        Cell mealRateCell = row.createCell(colNum++);
        mealRateCell.setCellStyle(currencyStyle);

        Cell mealAmountCell = row.createCell(colNum++);
        mealAmountCell.setCellStyle(currencyStyle);

        // Total amount (capped at 1300)
        double totalAmount = Math.min(fareAmount, MAX_TOTAL_AMOUNT);
        Cell totalCell = row.createCell(colNum++);
        totalCell.setCellValue(totalAmount);
        totalCell.setCellStyle(currencyStyle);

        // ID for signature
        Cell signIdCell = row.createCell(colNum++);
        signIdCell.setCellValue(student.getStudentID());
        signIdCell.setCellStyle(centerStyle);

        // Signature space
        Cell signatureCell = row.createCell(colNum);
        signatureCell.setCellStyle(dataStyle);

        return totalAmount;
    }

    private void createCertificationSection(Sheet sheet, int startRow) {
        CellStyle certStyle = createCertificationStyle(sheet.getWorkbook());

        Row certRow = sheet.createRow(startRow);
        Cell cert1 = certRow.createCell(0);
        cert1.setCellValue(
                "1. I HEREBY CERTIFY on my official oath to the correctness of the above roll. Payment is hereby approved from the appropriation indicated.");
        cert1.setCellStyle(certStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, 6));

        Cell cert2 = certRow.createCell(8);
        cert2.setCellValue("2. APPROVED");
        cert2.setCellStyle(certStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 8, 9));

        Cell cert3 = certRow.createCell(11);
        cert3.setCellValue(
                "3. I HEREBY CERTIFY on my official oath that I have this ___ day of ____ paid in cash to each man whose name appears on the above roll, the amount set opposite his name, he having presented himself, established his identity and affixed his signature or thumbmark on the space provided therefor.");
        cert3.setCellStyle(certStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 11, 31));

        // Signature lines
        Row signRow = sheet.createRow(startRow + 3);

        Cell sign1 = signRow.createCell(0);
        sign1.setCellValue("JESCYN KATE N. RAMOS, E2P - ICT Coordinator");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, 0, 6));

        Cell sign2 = signRow.createCell(8);
        sign2.setCellValue("CARLOS JERICHO L. PETILLA, Provincial Governor");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, 8, 9));

        Cell sign3 = signRow.createCell(11);
        sign3.setCellValue("JOAN FLORLYN JUSTISA, E2P - ICT");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, 11, 31));

        // Notes
        Row noteRow = sheet.createRow(startRow + 5);
        Cell note = noteRow.createCell(0);
        note.setCellValue(
                "NOTE: Where thumbmark is to be used in place of signature, and the space available is not sufficient, the thumbmark may be impressed on the back hereof with proper indication of the corresponding student's name and on the corresponding line on the payroll or remark 'see thumbmark on the back' should be written.");
        sheet.addMergedRegion(new CellRangeAddress(startRow + 5, startRow + 5, 0, 31));

        Row taglineRow = sheet.createRow(startRow + 6);
        Cell tagline = taglineRow.createCell(0);
        tagline.setCellValue("'IPAKITA SA MUNDO, UMAASENSA NA TAYO'");

        sheet.addMergedRegion(new CellRangeAddress(startRow + 6, startRow + 6, 0, 31));
    }

    // Cell style creation methods
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11); // Slightly smaller font for headers
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        addBorders(style);
        return style;
    }

    private CellStyle createSubHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(false);
        font.setFontHeightInPoints((short) 11); // Slightly smaller font for headers
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        addBorders(style);
        return style;
    }

    private CellStyle createCertificationStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(false);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        addBorders(style);
        return style;
    }

    private CellStyle createCenterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        addBorders(style);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat(PESO + "#,##0.00"));
        addBorders(style);
        return style;
    }

    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14); // Set font size to 12 points
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        addBorders(style);
        return style;
    }

    // Utility methods
    private void addBorders(CellStyle style) {
        // Use consistent THIN borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // Ensure borders are black
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
    }
}