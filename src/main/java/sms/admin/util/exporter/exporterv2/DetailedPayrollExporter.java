package sms.admin.util.exporter.exporterv2;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import sms.admin.util.attendance.CommonAttendanceUtil;

/**
 * Exports detailed payroll data to an Excel file.
 */
public class DetailedPayrollExporter {

    // Constants
    private static final double MAX_TOTAL_AMOUNT = 1300.00;

    // Instance variables
    private final YearMonth month;
    private final ObservableList<AttendanceLog> attendanceLogs;
    private double fareMultiplier = 1.0;
    private int totalDaysColumn;
    private int totalAmountColumn;

    /**
     * Constructor for DetailedPayrollExporter.
     *
     * @param month The month for which the payroll is being generated.
     * @param endMonth The end month (not used in this implementation).
     * @param logs The list of attendance logs.
     */
    public DetailedPayrollExporter(YearMonth month, YearMonth endMonth, ObservableList<AttendanceLog> logs) {
        this.month = month;
        this.attendanceLogs = logs;
    }

    /**
     * Sets the fare multiplier.
     *
     * @param multiplier The multiplier to apply to the fare.
     */
    public void setFareMultiplier(double multiplier) {
        this.fareMultiplier = multiplier;
    }

    /**
     * Exports the payroll data to an Excel file.
     *
     * @param table The TableView containing student data.
     * @param title The title of the sheet.
     * @param outputPath The path to save the Excel file.
     * @throws Exception If an error occurs during export.
     */
    public void exportToExcel(TableView<Student> table, String title, String outputPath) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(title);

            // Create cell styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);

            // Style for subtotal label (right-aligned)
            CellStyle subtotalLabelStyle = workbook.createCellStyle();
            Font subtotalFont = workbook.createFont();
            subtotalFont.setBold(true);
            subtotalFont.setFontHeightInPoints((short) 14);
            subtotalLabelStyle.setFont(subtotalFont);
            subtotalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
            subtotalLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            addBorders(subtotalLabelStyle);

            // Set column widths
            sheet.setColumnWidth(0, 5 * 256); // No. column
            sheet.setColumnWidth(1, 40 * 256); // Name column

            // Calculate weeks and total days
            List<List<LocalDate>> weeks = getWorkWeeks();
            int totalDays = weeks.stream().mapToInt(List::size).sum();

            // Set uniform width for day columns
            final int DAY_COLUMN_WIDTH = 3 * 256;
            for (int i = 2; i < 2 + totalDays; i++) {
                sheet.setColumnWidth(i, DAY_COLUMN_WIDTH);
            }

            // Set widths for remaining columns
            int totalDaysCol = 2 + totalDays;
            sheet.setColumnWidth(totalDaysCol, 5 * 256); // Total Days
            sheet.setColumnWidth(totalDaysCol + 1, 7 * 256); // Trans Daily Rate
            sheet.setColumnWidth(totalDaysCol + 2, 7 * 256); // Trans Amount Due
            sheet.setColumnWidth(totalDaysCol + 3, 7 * 256); // Meal Daily Rate
            sheet.setColumnWidth(totalDaysCol + 4, 7 * 256); // Meal Amount Due
            int totalAmountCol = totalDaysCol + 5;
            sheet.setColumnWidth(totalAmountCol, 8 * 256); // Total Amount
            sheet.setColumnWidth(totalAmountCol + 1, 4 * 256); // No. for signature
            sheet.setColumnWidth(totalAmountCol + 2, 20 * 256); // Signature

            // Define row heights
            short normalRowHeight = (short) (25 * 20);
            short headerRowHeight = (short) (30 * 20);
            short titleRowHeight = (short) (35 * 20);

            // Title row setup
            Row titleRow = sheet.createRow(0);
            titleRow.setHeight(titleRowHeight);
            Cell formCell = titleRow.createCell(0);
            formCell.setCellValue("GENERAL FORM NO. 7(A)");
            CellStyle formStyle = workbook.createCellStyle();
            Font formFont = workbook.createFont();
            formFont.setBold(true);
            formFont.setFontHeightInPoints((short) 11);
            formStyle.setFont(formFont);
            formStyle.setAlignment(HorizontalAlignment.LEFT);
            formCell.setCellStyle(formStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            Cell mainTitleCell = titleRow.createCell(8);
            mainTitleCell.setCellValue("TIME BOOK AND PAYROLL");
            CellStyle mainTitleStyle = workbook.createCellStyle();
            Font mainTitleFont = workbook.createFont();
            mainTitleFont.setBold(true);
            mainTitleFont.setFontHeightInPoints((short) 16);
            mainTitleStyle.setFont(mainTitleFont);
            mainTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            mainTitleCell.setCellStyle(mainTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 8, 26));

            // Period description row
            Row periodRow = sheet.createRow(1);
            periodRow.setHeight(headerRowHeight);

// Create the full text for the period cell
            String fullText = "For labor on _________ - Baybay Data Center, at Baybay National High School, Baybay City, Leyte, Philippines, for the period, "
                    + month.getMonth().toString() + " " + month.getYear();

// Create RichTextString for underlining specific parts
            XSSFRichTextString richText = new XSSFRichTextString(fullText);

// Create fonts for underlined and non-underlined text
            Font regularFont = workbook.createFont();
            regularFont.setFontHeightInPoints((short) 14);

            Font underlineFont = workbook.createFont();
            underlineFont.setFontHeightInPoints((short) 14);
            underlineFont.setUnderline(Font.U_SINGLE);

// Identify the substrings to underline
            String dataCenter = "Baybay Data Center";
            String schoolLocation = "Baybay National High School, Baybay City, Leyte";
            String monthYear = month.getMonth().toString() + " " + month.getYear();

// Find starting indices of each substring
            int dataCenterStart = fullText.indexOf(dataCenter);
            int schoolLocationStart = fullText.indexOf(schoolLocation);
            int monthYearStart = fullText.indexOf(monthYear);

// Apply regular font to the entire text initially
            richText.applyFont(0, fullText.length(), regularFont);

// Apply underline font to specific substrings
            if (dataCenterStart >= 0) {
                richText.applyFont(dataCenterStart, dataCenterStart + dataCenter.length(), underlineFont);
            }
            if (schoolLocationStart >= 0) {
                richText.applyFont(schoolLocationStart, schoolLocationStart + schoolLocation.length(), underlineFont);
            }
            if (monthYearStart >= 0) {
                richText.applyFont(monthYearStart, monthYearStart + monthYear.length(), underlineFont);
            }

// Set the rich text in the cell
            Cell periodCell = periodRow.createCell(0);
            periodCell.setCellValue(richText);

// Apply cell style
            CellStyle periodStyle = workbook.createCellStyle();
            periodStyle.setFont(regularFont);
            periodStyle.setAlignment(HorizontalAlignment.CENTER);
            periodStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            periodCell.setCellStyle(periodStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 34));

            // Spacer row
            sheet.createRow(2).setHeight((short) (15 * 20));

            // Store column indices
            totalDaysColumn = totalDaysCol;
            totalAmountColumn = totalAmountCol;

            // Create table headers
            totalDays = createTableHeaders(sheet, 3, headerStyle, centerStyle, weeks);

            // Write student data rows
            int rowNum = 7;
            double grandTotal = 0;
            int noCounter = 1;
            for (Student student : table.getItems()) {
                Row row = sheet.createRow(rowNum++);
                row.setHeight(normalRowHeight);
                grandTotal += writeStudentRow(row, student, weeks, dataStyle, currencyStyle, centerStyle, noCounter++);
            }

            // Subtotal row
            Row subtotalRow = sheet.createRow(rowNum++);
            subtotalRow.setHeight(normalRowHeight);
            Cell subtotalLabel = subtotalRow.createCell(0);
            subtotalLabel.setCellValue("SUB - TOTAL FOR THIS PAGE:  ");
            subtotalLabel.setCellStyle(subtotalLabelStyle);
            sheet.addMergedRegion(
                    new CellRangeAddress(subtotalRow.getRowNum(), subtotalRow.getRowNum(), 0, totalAmountCol - 1));
            Cell subtotalAmount = subtotalRow.createCell(totalAmountCol);
            subtotalAmount.setCellValue(grandTotal);
            subtotalAmount.setCellStyle(currencyStyle);

            // Certification section
            rowNum += 2;
            createCertificationSection(sheet, rowNum);

            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            }
        }
    }

    /**
     * Retrieves work weeks for the specified month, excluding weekends.
     *
     * @return A list of lists, where each inner list represents a week of
     * working days.
     */
    private List<List<LocalDate>> getWorkWeeks() {
        List<LocalDate> workingDays = new ArrayList<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            if (!CommonAttendanceUtil.isWeekend(date)) {
                workingDays.add(date);
            }
        }
        List<List<LocalDate>> weeks = new ArrayList<>();
        int start = 0;
        while (start < workingDays.size()) {
            int end = Math.min(start + 5, workingDays.size());
            weeks.add(new ArrayList<>(workingDays.subList(start, end)));
            start += 5;
        }
        return weeks;
    }

    /**
     * Determines the attendance status for a student on a specific date.
     *
     * @param student The student.
     * @param date The date to check.
     * @return The attendance status (e.g., "P", "A").
     */
    private String getAttendanceStatus(Student student, LocalDate date) {
        return attendanceLogs.stream()
                .filter(log -> log != null
                && log.getStudentID() != null
                && log.getStudentID().getStudentID() == student.getStudentID()
                && log.getRecordID() != null
                && log.getRecordID().getYear() == date.getYear()
                && log.getRecordID().getMonth() == date.getMonthValue()
                && log.getRecordID().getDay() == date.getDayOfMonth())
                .map(log -> {
                    String status = CommonAttendanceUtil.computeAttendanceStatus(log);
                    return status.equals(CommonAttendanceUtil.HALF_DAY_MARK) ? CommonAttendanceUtil.ABSENT_MARK : status;
                })
                .findFirst().orElse(CommonAttendanceUtil.ABSENT_MARK);

    }

    /**
     * Calculates the total number of days a student was present or had excused
     * absences.
     *
     * @param student The student.
     * @param month The month to calculate for.
     * @return The total days attended.
     */
    public double calculateStudentDays(Student student, YearMonth month) {
        try {
            double totalDays = 0;
            List<AttendanceLog> studentLogs = attendanceLogs.stream()
                    .filter(log -> log != null
                    && log.getStudentID() != null
                    && log.getStudentID().getStudentID() == student.getStudentID()
                    && log.getRecordID() != null
                    && YearMonth.of(log.getRecordID().getYear(), log.getRecordID().getMonth()).equals(month)
                    && !CommonAttendanceUtil.isWeekend(LocalDate.of(
                            log.getRecordID().getYear(), log.getRecordID().getMonth(),
                            log.getRecordID().getDay())))
                    .collect(Collectors.toList());

            for (AttendanceLog log : studentLogs) {
                String status = CommonAttendanceUtil.computeAttendanceStatus(log);
                switch (status) {
                    case CommonAttendanceUtil.PRESENT_MARK, CommonAttendanceUtil.EXCUSED_MARK, CommonAttendanceUtil.HOLIDAY_MARK ->
                        totalDays += 1.0;
                    case CommonAttendanceUtil.HALF_DAY_MARK ->
                        totalDays += 0;
                }
            }
            return totalDays;
        } catch (Exception e) {
            System.err.println("Error calculating days for student " + student.getStudentID());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Formats a student's full name.
     *
     * @param student The student.
     * @return The formatted full name (e.g., "Doe, John A Jr").
     */
    private String formatFullName(Student student) {
        String lastName = capitalizeWord(student.getLastName());
        String firstName = capitalizeWord(student.getFirstName());
        String middleName = student.getMiddleName() != null ? capitalizeWord(student.getMiddleName()) + " " : "";
        String extension = student.getNameExtension() != null ? " " + student.getNameExtension().toUpperCase() : "";
        return String.format("%s, %s %s%s", lastName, firstName, middleName, extension);
    }

    /**
     * Capitalizes the first letter of each word in a string.
     *
     * @param word The word to capitalize.
     * @return The capitalized word.
     */
    private String capitalizeWord(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        String[] parts = word.toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Formats the day name (e.g., "M" for Monday).
     *
     * @param date The date.
     * @return The formatted day initial.
     */
    private String formatDayName(LocalDate date) {
        return CommonAttendanceUtil.getDayInitial(date.getDayOfWeek());
    }

    /**
     * Creates the table headers in the Excel sheet.
     *
     * @param sheet The sheet to modify.
     * @param startRow The starting row for headers.
     * @param headerStyle Style for header cells.
     * @param centerStyle Style for centered text.
     * @param weeks List of work weeks.
     * @return The total number of days.
     */
    private int createTableHeaders(Sheet sheet, int startRow, CellStyle headerStyle,
            CellStyle centerStyle, List<List<LocalDate>> weeks) {
        Row headerRow1 = sheet.createRow(startRow);
        Row headerRow2 = sheet.createRow(startRow + 1);
        Row headerRow3 = sheet.createRow(startRow + 2);
        Row headerRow4 = sheet.createRow(startRow + 3);

        // No. and Name headers
        Cell noHeader = headerRow1.createCell(0);
        noHeader.setCellValue("No.");
        noHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, 0, 0));

        Cell nameHeader = headerRow1.createCell(1);
        nameHeader.setCellValue("Student Name");
        nameHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, 1, 1));

        // Time roll header
        int currentCol = 2;
        Cell timeRollHeader = headerRow1.createCell(currentCol);
        timeRollHeader.setCellValue("TIME ROLL");
        timeRollHeader.setCellStyle(headerStyle);

        int totalDays = weeks.stream().mapToInt(List::size).sum();
        int maxDayDigits = String.valueOf(month.lengthOfMonth()).length();
        final int TIME_ROLL_WIDTH = (maxDayDigits + 2) * 256;

        for (int i = 0; i < totalDays; i++) {
            sheet.setColumnWidth(currentCol + i, TIME_ROLL_WIDTH);
        }
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, currentCol, currentCol + totalDays - 1));

        // Week and day headers
        int weekStartCol = currentCol;
        int weekNum = 1;
        for (List<LocalDate> week : weeks) {
            if (!week.isEmpty()) {
                int daysInWeek = week.size();
                Cell weekHeader = headerRow2.createCell(weekStartCol);
                weekHeader.setCellValue("Week " + weekNum++);
                weekHeader.setCellStyle(centerStyle);

                if (daysInWeek > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 1, weekStartCol,
                            weekStartCol + daysInWeek - 1));
                }

                for (int i = 0; i < daysInWeek; i++) {
                    LocalDate date = week.get(i);
                    Cell dayNameCell = headerRow3.createCell(weekStartCol + i);
                    dayNameCell.setCellValue(formatDayName(date));
                    dayNameCell.setCellStyle(centerStyle);

                    Cell dayCell = headerRow4.createCell(weekStartCol + i);
                    dayCell.setCellValue(date.getDayOfMonth());
                    dayCell.setCellStyle(centerStyle);
                }
                weekStartCol += daysInWeek;
            }
        }

        // Remaining headers
        int remainingCol = weekStartCol;
        sheet.setColumnWidth(remainingCol, 6 * 256);
        int allowanceWidth = 9 * 256;
        sheet.setColumnWidth(remainingCol + 1, allowanceWidth);
        sheet.setColumnWidth(remainingCol + 2, allowanceWidth);
        sheet.setColumnWidth(remainingCol + 3, allowanceWidth);
        sheet.setColumnWidth(remainingCol + 4, allowanceWidth);
        sheet.setColumnWidth(remainingCol + 5, 10 * 256);
        sheet.setColumnWidth(remainingCol + 6, 4 * 256);
        sheet.setColumnWidth(remainingCol + 7, 20 * 256);

        addRemainingHeaders(sheet, startRow, remainingCol, headerStyle, centerStyle);
        return totalDays;
    }

    /**
     * Adds remaining headers to the sheet (Total Days, Allowances, etc.).
     *
     * @param sheet The sheet.
     * @param startRow Starting row for headers.
     * @param startCol Starting column for remaining headers.
     * @param headerStyle Style for headers.
     * @param centerStyle Style for centered text.
     */
    private void addRemainingHeaders(Sheet sheet, int startRow, int startCol,
            CellStyle headerStyle, CellStyle centerStyle) {
        CellStyle subHeaderStyle = createSubHeaderStyle(sheet.getWorkbook());
        int currentCol = startCol;

        Cell totalDaysHeader = sheet.getRow(startRow).createCell(currentCol);
        totalDaysHeader.setCellValue("Total Days");
        totalDaysHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, currentCol, currentCol));
        currentCol++;

        Cell transHeader = sheet.getRow(startRow).createCell(currentCol);
        transHeader.setCellValue("Transportation Allowance");
        transHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, currentCol, currentCol + 1));

        Cell transRateHeader = sheet.getRow(startRow + 1).createCell(currentCol);
        transRateHeader.setCellValue("Daily Rate");
        transRateHeader.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 3, currentCol, currentCol));

        Cell transAmountHeader = sheet.getRow(startRow + 1).createCell(currentCol + 1);
        transAmountHeader.setCellValue("Amount Due");
        transAmountHeader.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 3, currentCol + 1, currentCol + 1));
        currentCol += 2;

        Cell mealHeader = sheet.getRow(startRow).createCell(currentCol);
        mealHeader.setCellValue("Meal Allowance");
        mealHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, currentCol, currentCol + 1));

        Cell mealRateHeader = sheet.getRow(startRow + 1).createCell(currentCol);
        mealRateHeader.setCellValue("Daily Rate");
        mealRateHeader.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 3, currentCol, currentCol));

        Cell mealAmountHeader = sheet.getRow(startRow + 1).createCell(currentCol + 1);
        mealAmountHeader.setCellValue("Amount Due");
        mealAmountHeader.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 3, currentCol + 1, currentCol + 1));
        currentCol += 2;

        Cell totalAmountHeader = sheet.getRow(startRow).createCell(totalAmountColumn);
        totalAmountHeader.setCellValue("Total Amount Received");
        totalAmountHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, totalAmountColumn, totalAmountColumn));
        currentCol = totalAmountColumn + 1;

        Cell signNoHeader = sheet.getRow(startRow).createCell(currentCol);
        signNoHeader.setCellValue("No.");
        signNoHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, currentCol, currentCol));
        currentCol++;

        Cell signatureHeader = sheet.getRow(startRow).createCell(currentCol);
        signatureHeader.setCellValue("Signature");
        signatureHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, currentCol, currentCol));
    }

    /**
     * Writes a row of data for a student.
     *
     * @param row The row to write to.
     * @param student The student data.
     * @param weeks List of work weeks.
     * @param dataStyle Style for data cells.
     * @param currencyStyle Style for currency cells.
     * @param centerStyle Style for centered text.
     * @param no Student number.
     * @return The total amount for the student.
     */
    private double writeStudentRow(Row row, Student student, List<List<LocalDate>> weeks,
            CellStyle dataStyle, CellStyle currencyStyle, CellStyle centerStyle, int no) {
        int colNum = 0;

        Cell idCell = row.createCell(colNum++);
        idCell.setCellValue(no);
        idCell.setCellStyle(centerStyle);

        Cell nameCell = row.createCell(colNum++);
        nameCell.setCellValue(formatFullName(student));
        nameCell.setCellStyle(centerStyle);

        double totalDays = 0;
        for (List<LocalDate> week : weeks) {
            for (LocalDate date : week) {
                Cell attendanceCell = row.createCell(colNum++);
                String status = getAttendanceStatus(student, date);
                attendanceCell.setCellValue(status);
                switch (status) {
                    case CommonAttendanceUtil.PRESENT_MARK, CommonAttendanceUtil.EXCUSED_MARK, CommonAttendanceUtil.HOLIDAY_MARK ->
                        totalDays++;
                }
                attendanceCell.setCellStyle(centerStyle);
            }
        }

        Cell totalDaysCell = row.createCell(colNum++);
        totalDaysCell.setCellValue(totalDays);
        totalDaysCell.setCellStyle(centerStyle);

        double fareRate = student.getFare() * fareMultiplier;
        double fareAmount = fareRate * totalDays;
        double totalAmount = Math.min(fareAmount, MAX_TOTAL_AMOUNT);

        Cell fareRateCell = row.createCell(colNum++);
        fareRateCell.setCellValue(fareRate);
        fareRateCell.setCellStyle(currencyStyle);

        Cell fareAmountCell = row.createCell(colNum++);
        fareAmountCell.setCellValue(fareAmount);
        fareAmountCell.setCellStyle(currencyStyle);

        Cell mealRateCell = row.createCell(colNum++);
        mealRateCell.setCellStyle(currencyStyle);

        Cell mealAmountCell = row.createCell(colNum++);
        mealAmountCell.setCellStyle(currencyStyle);

        Cell totalCell = row.createCell(colNum++);
        totalCell.setCellValue(totalAmount);
        totalCell.setCellStyle(currencyStyle);

        Cell signIdCell = row.createCell(colNum++);
        signIdCell.setCellValue(no);
        signIdCell.setCellStyle(centerStyle);

        Cell signatureCell = row.createCell(colNum);
        signatureCell.setCellStyle(dataStyle);

        return totalAmount;
    }

    /**
     * Creates the certification section at the bottom of the sheet.
     *
     * @param sheet The sheet to modify.
     * @param startRow The starting row for the certification.
     */
    private void createCertificationSection(Sheet sheet, int startRow) {
        CellStyle certStyle = sheet.getWorkbook().createCellStyle();
        Font certFont = sheet.getWorkbook().createFont();
        certFont.setFontHeightInPoints((short) 10);
        certStyle.setFont(certFont);
        certStyle.setAlignment(HorizontalAlignment.CENTER);
        certStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        certStyle.setWrapText(true);

        CellStyle nameStyle = sheet.getWorkbook().createCellStyle();
        Font nameFont = sheet.getWorkbook().createFont();
        nameFont.setBold(true);
        nameFont.setUnderline(Font.U_SINGLE);
        nameFont.setFontHeightInPoints((short) 10);
        nameStyle.setFont(nameFont);
        nameStyle.setAlignment(HorizontalAlignment.CENTER);
        nameStyle.setVerticalAlignment(VerticalAlignment.BOTTOM);
        nameStyle.setWrapText(true);

        CellStyle roleStyle = sheet.getWorkbook().createCellStyle();
        Font roleFont = sheet.getWorkbook().createFont();
        roleFont.setFontHeightInPoints((short) 10);
        roleStyle.setFont(roleFont);
        roleStyle.setAlignment(HorizontalAlignment.CENTER);
        roleStyle.setVerticalAlignment(VerticalAlignment.TOP);
        roleStyle.setWrapText(true);

        Row certRow = sheet.createRow(startRow);
        certRow.setHeight((short) (50 * 20));

        Cell cert1 = certRow.createCell(0);
        cert1.setCellValue(
                "1. I HEREBY CERTIFY on my official oath to the correctness of the above roll. Payment is hereby approved from the appropriation indicated.");
        cert1.setCellStyle(certStyle);
        int cert1EndColumn = Math.max(7, totalDaysColumn / 3);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, cert1EndColumn));

        int cert2StartColumn = cert1EndColumn + 1;
        int cert2EndColumn = cert2StartColumn + 8;

        Cell cert2 = certRow.createCell(cert2StartColumn);
        cert2.setCellValue("2. APPROVED");
        cert2.setCellStyle(certStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, cert2StartColumn, cert2EndColumn));

        Row nameRow = sheet.createRow(startRow + 3);
        Row roleRow = sheet.createRow(startRow + 4);
        nameRow.setHeight((short) (20 * 20));
        roleRow.setHeight((short) (15 * 20));

        Cell name1 = nameRow.createCell(0);
        Cell role1 = roleRow.createCell(0);
        name1.setCellValue("JESCYN KATE N. RAMOS");
        role1.setCellValue("E2P - ICT Coordinator");
        name1.setCellStyle(nameStyle);
        role1.setCellStyle(roleStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, 0, cert1EndColumn));
        sheet.addMergedRegion(new CellRangeAddress(startRow + 4, startRow + 4, 0, cert1EndColumn));

        Cell name2 = nameRow.createCell(cert2StartColumn);
        Cell role2 = roleRow.createCell(cert2StartColumn);
        name2.setCellValue("CARLOS JERICHO L. PETILLA");
        role2.setCellValue("Provincial Governor");
        name2.setCellStyle(nameStyle);
        role2.setCellStyle(roleStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, cert2StartColumn, cert2EndColumn));
        sheet.addMergedRegion(new CellRangeAddress(startRow + 4, startRow + 4, cert2StartColumn, cert2EndColumn));

        Cell cert3 = certRow.createCell(cert2EndColumn + 1);
        cert3.setCellValue(
                "3. I HEREBY CERTIFY on my official oath that I have this ___ day of ____ paid in cash to each man whose name appears on the above roll, the amount set opposite his name, he having presented himself, established his identity and affixed his signature or thumbmark on the space provided therefor.");
        cert3.setCellStyle(certStyle);
        int cert3EndColumn = totalAmountColumn + 2; // Keep original end column for cert3 text
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, cert2EndColumn + 1, cert3EndColumn));

        // Move name3 and role3 to a fixed column (e.g., column 20) to shift left
        int cert3NameStartColumn = 20; // Adjust as needed
        int cert3NameEndColumn = cert3NameStartColumn + 8; // Span 8 columns for width
        Cell name3 = nameRow.createCell(cert3NameStartColumn);
        Cell role3 = roleRow.createCell(cert3NameStartColumn);
        name3.setCellValue("JOAN FLORLYN JUSTISA");
        role3.setCellValue("E2P - ICT");
        name3.setCellStyle(nameStyle);
        role3.setCellStyle(roleStyle);
        sheet.addMergedRegion(
                new CellRangeAddress(startRow + 3, startRow + 3, cert3NameStartColumn, cert3NameEndColumn));
        sheet.addMergedRegion(
                new CellRangeAddress(startRow + 4, startRow + 4, cert3NameStartColumn, cert3NameEndColumn));

        Row noteRow = sheet.createRow(startRow + 6);
        noteRow.setHeight((short) (30 * 20));
        Cell note = noteRow.createCell(0);
        note.setCellValue(
                "*NOTE: Where thumbmark is to be used in place of signature, and the space available is not sufficient, the thumbmark may be impressed on the back hereof with proper indication of the corresponding student's number and on the corresponding line on the payroll\n"
                + "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0or remark 'see thumbmark on the back' should be written.");
        CellStyle noteStyle = sheet.getWorkbook().createCellStyle();
        noteStyle.cloneStyleFrom(certStyle);
        noteStyle.setWrapText(true);
        noteStyle.setAlignment(HorizontalAlignment.LEFT);
        noteStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        note.setCellStyle(noteStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 6, startRow + 6, 0, totalAmountColumn + 2));

        Row taglineRow = sheet.createRow(startRow + 7);
        Cell tagline = taglineRow.createCell(0);
        taglineRow.setHeight((short) (30 * 20));
        tagline.setCellValue("\"IPAKITA SA MUNDO, UMAASENSO NA TAYO\"");
        CellStyle taglineStyle = sheet.getWorkbook().createCellStyle();
        Font taglineFont = sheet.getWorkbook().createFont();
        taglineFont.setBold(true);
        taglineFont.setFontHeightInPoints((short) 16);
        taglineStyle.setFont(taglineFont);
        taglineStyle.setAlignment(HorizontalAlignment.CENTER);
        taglineStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        tagline.setCellStyle(taglineStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 7, startRow + 7, 0, totalAmountColumn + 2));
    }

    /**
     * Creates a header style for the workbook.
     *
     * @param workbook The workbook.
     * @return The header cell style.
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        addBorders(style);
        return style;
    }

    /**
     * Creates a sub-header style for the workbook.
     *
     * @param workbook The workbook.
     * @return The sub-header cell style.
     */
    private CellStyle createSubHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(false);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        addBorders(style);
        return style;
    }

    /**
     * Creates a data style for the workbook.
     *
     * @param workbook The workbook.
     * @return The data cell style.
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        addBorders(style);
        return style;
    }

    /**
     * Creates a centered text style for the workbook.
     *
     * @param workbook The workbook.
     * @return The centered cell style.
     */
    private CellStyle createCenterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        addBorders(style);
        return style;
    }

    /**
     * Creates a currency style for the workbook.
     *
     * @param workbook The workbook.
     * @return The currency cell style.
     */
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        addBorders(style);
        return style;
    }

    /**
     * Creates a total style for the workbook (unused in this implementation).
     *
     * @param workbook The workbook.
     * @return The total cell style.
     */
    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        addBorders(style);
        return style;
    }

    /**
     * Adds borders to a cell style.
     *
     * @param style The style to modify.
     */
    private void addBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
    }
}
