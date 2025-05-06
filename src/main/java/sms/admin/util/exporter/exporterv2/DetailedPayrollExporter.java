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
     * @param month    The month for which the payroll is being generated.
     * @param endMonth The end month (not used in this implementation).
     * @param logs     The list of attendance logs.
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

    private CellStyle createSubtotalLabelStyle(Workbook workbook) {
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

    public void exportToExcel(TableView<Student> table, String title, String outputPath) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(title);

            // Create cell styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);
            CellStyle subtotalLabelStyle = createSubtotalLabelStyle(workbook);

            // Define column indices
            int labelCol = 2;
            int timeRollStartCol = 3;

            // Set column widths
            sheet.setColumnWidth(0, 5 * 256);
            sheet.setColumnWidth(1, 40 * 256);
            sheet.setColumnWidth(labelCol, 6 * 256); // 256 units = 1 character width

            // Calculate weeks and total days
            List<List<LocalDate>> weeks = getWorkWeeks();
            int totalDays = weeks.stream().mapToInt(List::size).sum();

            // Set uniform width for day columns
            final int DAY_COLUMN_WIDTH = 3 * 256;
            for (int i = timeRollStartCol; i < timeRollStartCol + totalDays; i++) {
                sheet.setColumnWidth(i, DAY_COLUMN_WIDTH);
            }

            // Set widths for remaining columns
            int totalDaysCol = timeRollStartCol + totalDays;
            sheet.setColumnWidth(totalDaysCol, 5 * 256);
            sheet.setColumnWidth(totalDaysCol + 1, 7 * 256);
            sheet.setColumnWidth(totalDaysCol + 2, 7 * 256);
            sheet.setColumnWidth(totalDaysCol + 3, 7 * 256);
            sheet.setColumnWidth(totalDaysCol + 4, 7 * 256);
            int totalAmountCol = totalDaysCol + 5;
            sheet.setColumnWidth(totalAmountCol, 8 * 256);
            sheet.setColumnWidth(totalAmountCol + 1, 4 * 256);
            sheet.setColumnWidth(totalAmountCol + 2, 20 * 256);

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
            formFont.setBold(false);
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
            mainTitleFont.setFontHeightInPoints((short) 18);
            mainTitleStyle.setFont(mainTitleFont);
            mainTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            mainTitleCell.setCellStyle(mainTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 8, 26));

            // Period description row
            Row periodRow = sheet.createRow(1);
            periodRow.setHeight(headerRowHeight);

            // Create the full text for the period cell
            String fullText = "For labor on _________ - \u00A0\u00A0 Baybay Data Center \u00A0\u00A0 ,at \u00A0\u00A0 Baybay Nat'l High School, Baybay City, Leyte \u00A0\u00A0  ,Philippines, for the period,\u00A0\u00A0\u00A0"
                    + "  \u00A0\u00A0 " + month.getMonth().toString() + " " + month.getYear() + "\u00A0\u00A0\u00A0";

            // Create RichTextString for underlining specific parts
            XSSFRichTextString richText = new XSSFRichTextString(fullText);

            // Create fonts for underlined and non-underlined text
            Font regularFont = workbook.createFont();
            regularFont.setFontHeightInPoints((short) 16);

            Font underlineFont = workbook.createFont();
            underlineFont.setFontHeightInPoints((short) 16);
            underlineFont.setUnderline(Font.U_SINGLE);

            // Identify the substrings to underline
            String dataCenter = " \u00A0\u00A0 Baybay Data Center \u00A0\u00A0 ";
            String schoolLocation = " \u00A0\u00A0 Baybay Nat'l High School, Baybay City, Leyte \u00A0\u00A0  ";
            String monthYear = "  \u00A0\u00A0 " + month.getMonth().toString() + " " + month.getYear()
                    + "\u00A0\u00A0\u00A0";

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

            // Create table headers with new label column
            createTableHeaders(sheet, 3, headerStyle, centerStyle, weeks, labelCol, timeRollStartCol);

            // Write student data rows
            int rowNum = 7;
            double grandTotal = 0;
            int noCounter = 1;
            for (Student student : table.getItems()) {
                Row row = sheet.createRow(rowNum++);
                row.setHeight((short) (25 * 20));
                grandTotal += writeStudentRow(row, student, weeks, dataStyle, currencyStyle,
                        centerStyle, noCounter++, timeRollStartCol,
                        totalDaysCol, totalAmountCol);
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
     *         working days.
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
     * @param date    The date to check.
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
                    return status.equals(CommonAttendanceUtil.HALF_DAY_MARK) ? CommonAttendanceUtil.ABSENT_MARK
                            : status;
                })
                .findFirst().orElse(CommonAttendanceUtil.ABSENT_MARK);

    }

    /**
     * Calculates the total number of days a student was present or had excused
     * absences.
     *
     * @param student The student.
     * @param month   The month to calculate for.
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
                    case CommonAttendanceUtil.PRESENT_MARK, CommonAttendanceUtil.EXCUSED_MARK ->
                        totalDays += 1.0;
                    case CommonAttendanceUtil.HALF_DAY_MARK ->
                        totalDays += 0;
                    // Holiday is treated as absent by default since it's not included in the switch
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
     * @param sheet            The sheet to modify.
     * @param startRow         The starting row for headers.
     * @param headerStyle      Style for header cells.
     * @param centerStyle      Style for centered text.
     * @param weeks            List of work weeks.
     * @param labelCol         The column index for the label column.
     * @param timeRollStartCol The starting column index for the time roll.
     */
    private void createTableHeaders(Sheet sheet, int startRow, CellStyle headerStyle,
            CellStyle centerStyle, List<List<LocalDate>> weeks, int labelCol, int timeRollStartCol) {
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

        // Calculate total days for merging "Time Roll"
        int totalDays = weeks.stream().mapToInt(List::size).sum();

        // Time roll data columns setup
        int currentCol = timeRollStartCol;
        int maxDayDigits = String.valueOf(month.lengthOfMonth()).length();
        final int TIME_ROLL_WIDTH = (maxDayDigits + 2) * 256;

        // Set uniform width and create cells
        for (int i = 0; i < Math.max(totalDays, 1); i++) {
            sheet.setColumnWidth(currentCol + i, TIME_ROLL_WIDTH);
            Cell cell1 = headerRow1.createCell(currentCol + i);
            cell1.setCellStyle(headerStyle);
            Cell cell2 = headerRow2.createCell(currentCol + i);
            cell2.setCellStyle(centerStyle);
            Cell cell3 = headerRow3.createCell(currentCol + i);
            cell3.setCellStyle(centerStyle);
            Cell cell4 = headerRow4.createCell(currentCol + i);
            cell4.setCellStyle(centerStyle);
        }

        // Handle Time Roll header
        Cell timeRollCell = headerRow1.createCell(labelCol);
        timeRollCell.setCellStyle(headerStyle);
        if (totalDays >= 2) {
            timeRollCell.setCellValue("TIME ROLL");
            // Create and style remaining cells
            for (int c = labelCol + 1; c < timeRollStartCol + totalDays; c++) {
                Cell cell = headerRow1.getCell(c);
                if (cell == null) {
                    cell = headerRow1.createCell(c);
                }
                cell.setCellStyle(headerStyle);
            }

            // Single merge for time roll header
            try {
                CellRangeAddress timeRollRegion = new CellRangeAddress(
                        startRow, startRow,
                        labelCol, timeRollStartCol + totalDays - 1);
                sheet.addMergedRegion(timeRollRegion);
            } catch (IllegalStateException e) {
                System.err.println("Warning: Could not merge time roll header - region may already exist");
            }
        } else if (totalDays == 1) {
            timeRollCell.setCellValue("TIME ROLL");
        } else {
            timeRollCell.setCellValue("NO WORKING DAYS");
        }

        // Week headers and day/date entries
        int weekCounter = 1;
        for (List<LocalDate> week : weeks) {
            if (!week.isEmpty()) {
                int daysInWeek = week.size();

                // Create and style all cells in the merged region
                if (daysInWeek > 1) {
                    try {
                        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 1,
                                currentCol, currentCol + daysInWeek - 1));
                    } catch (IllegalStateException e) {
                        System.err.println("Warning: Could not merge week header - region may already exist");
                    }
                }

                // Apply style and set week number
                for (int c = currentCol; c <= currentCol + daysInWeek - 1; c++) {
                    Cell weekCell = headerRow2.getCell(c);
                    if (weekCell == null) {
                        weekCell = headerRow2.createCell(c);
                    }
                    weekCell.setCellStyle(centerStyle);
                }
                headerRow2.getCell(currentCol).setCellValue(weekCounter++);

                // Create day and date cells
                for (int i = 0; i < daysInWeek; i++) {
                    LocalDate date = week.get(i);
                    Cell dayNameCell = headerRow3.getCell(currentCol + i);
                    dayNameCell.setCellValue(formatDayName(date));
                    dayNameCell.setCellStyle(centerStyle);

                    Cell dayCell = headerRow4.getCell(currentCol + i);
                    dayCell.setCellValue(date.getDayOfMonth());
                    dayCell.setCellStyle(centerStyle);
                }
                currentCol += daysInWeek;
            }
        }

        // Sub-labels in label column
        Cell weekLabel = headerRow2.createCell(labelCol);
        weekLabel.setCellValue("Week");
        weekLabel.setCellStyle(centerStyle);

        Cell dayLabel = headerRow3.createCell(labelCol);
        dayLabel.setCellValue("Day");
        dayLabel.setCellStyle(centerStyle);

        Cell dateLabel = headerRow4.createCell(labelCol);
        dateLabel.setCellValue("Date");
        dateLabel.setCellStyle(centerStyle);

        // Add remaining headers
        addRemainingHeaders(sheet, startRow, currentCol, headerStyle, centerStyle);
    }

    /**
     * Adds remaining headers to the sheet (Total Days, Allowances, etc.).
     *
     * @param sheet       The sheet.
     * @param startRow    Starting row for headers.
     * @param startCol    Starting column for remaining headers.
     * @param headerStyle Style for headers.
     * @param centerStyle Style for centered text.
     */
    private void addRemainingHeaders(Sheet sheet, int startRow, int startCol,
            CellStyle headerStyle, CellStyle centerStyle) {
        CellStyle subHeaderStyle = createSubHeaderStyle(sheet.getWorkbook());
        int currentCol = startCol;

        // Set wider column width for No.
        sheet.setColumnWidth(0, 12 * 256); // No. column widened to 12 characters

        // Set adjusted column widths (adding 4 characters)
        sheet.setColumnWidth(0, 9 * 256); // No. column (5 + 4 characters)

        Cell totalDaysHeader = sheet.getRow(startRow).createCell(currentCol);
        totalDaysHeader.setCellValue("Total Days");
        totalDaysHeader.setCellStyle(headerStyle);
        sheet.setColumnWidth(currentCol, 6 * 256); // Increased from 5 to 6 characters
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, currentCol, currentCol));
        currentCol++;

        // Transportation Allowance columns with adjusted widths
        Cell transHeader = sheet.getRow(startRow).createCell(currentCol);
        transHeader.setCellValue("Transportation Allowance");
        transHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, currentCol, currentCol + 1));

        // Set wider columns for Transportation
        sheet.setColumnWidth(currentCol, 11 * 256); // Daily Rate (7 + 4 characters)
        sheet.setColumnWidth(currentCol + 1, 11 * 256); // Amount Due (7 + 4 characters)

        Cell transRateHeader = sheet.getRow(startRow + 1).createCell(currentCol);
        transRateHeader.setCellValue("Daily Rate");
        transRateHeader.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 3, currentCol, currentCol));

        Cell transAmountHeader = sheet.getRow(startRow + 1).createCell(currentCol + 1);
        transAmountHeader.setCellValue("Amount Due");
        transAmountHeader.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 3, currentCol + 1, currentCol + 1));
        currentCol += 2;

        // Meal Allowance columns with adjusted widths
        Cell mealHeader = sheet.getRow(startRow).createCell(currentCol);
        mealHeader.setCellValue("Meal Allowance");
        mealHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, currentCol, currentCol + 1));

        // Set wider columns for Meal
        sheet.setColumnWidth(currentCol, 11 * 256); // Daily Rate (7 + 4 characters)
        sheet.setColumnWidth(currentCol + 1, 11 * 256); // Amount Due (7 + 4 characters)

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

        // Set wider column width for Total Amount Received
        sheet.setColumnWidth(totalAmountColumn, 15 * 256); // Total Amount column widened to 15 characters

        Cell signNoHeader = sheet.getRow(startRow).createCell(currentCol);
        signNoHeader.setCellValue("No.");
        signNoHeader.setCellStyle(headerStyle);
        sheet.setColumnWidth(currentCol, 5 * 256); // Increase width from 4 to 5 characters
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
     * @param row           The row to write to.
     * @param student       The student data.
     * @param weeks         List of work weeks.
     * @param dataStyle     Style for data cells.
     * @param currencyStyle Style for currency cells.
     * @param centerStyle   Style for centered text.
     * @param no            Student number.
     * @return The total amount for the student.
     */
    private double writeStudentRow(Row row, Student student, List<List<LocalDate>> weeks,
            CellStyle dataStyle, CellStyle currencyStyle, CellStyle centerStyle, int no,
            int timeRollStartCol, int totalDaysCol, int totalAmountCol) {
        int colNum = 0;

        // ID and Name cells
        Cell idCell = row.createCell(colNum++);
        idCell.setCellValue(no);
        idCell.setCellStyle(centerStyle);

        Cell nameCell = row.createCell(colNum++);
        nameCell.setCellValue(formatFullName(student));
        nameCell.setCellStyle(centerStyle);

        // Skip label column
        colNum = timeRollStartCol;
        double totalDays = 0;
        for (List<LocalDate> week : weeks) {
            for (LocalDate date : week) {
                Cell attendanceCell = row.createCell(colNum++);
                String status = getAttendanceStatus(student, date);
                attendanceCell.setCellValue(status);
                attendanceCell.setCellStyle(centerStyle);
                switch (status) {
                    case CommonAttendanceUtil.PRESENT_MARK, CommonAttendanceUtil.EXCUSED_MARK ->
                        totalDays++;
                }
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
     * @param sheet    The sheet to modify.
     * @param startRow The starting row for the certification.
     */
    private void createCertificationSection(Sheet sheet, int startRow) {
        CellStyle certStyle = sheet.getWorkbook().createCellStyle();
        Font certFont = sheet.getWorkbook().createFont();
        certFont.setFontHeightInPoints((short) 10);
        certStyle.setFont(certFont);
        certStyle.setAlignment(HorizontalAlignment.LEFT);
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

        CellStyle roleStyle = sheet.getWorkbook().createCellStyle();
        Font roleFont = sheet.getWorkbook().createFont();
        roleFont.setFontHeightInPoints((short) 10);
        roleStyle.setFont(roleFont);
        roleStyle.setAlignment(HorizontalAlignment.CENTER);
        roleStyle.setVerticalAlignment(VerticalAlignment.TOP);

        // Create certification rows with adjusted heights
        Row certRow = sheet.createRow(startRow);
        certRow.setHeight((short) (40 * 20));

        // Calculate column spans based on total width
        int totalColumns = totalAmountColumn + 2;
        int cert1Width = totalColumns / 3;
        int cert2Width = totalColumns / 3;
        int cert3Width = totalColumns - cert1Width - cert2Width;

        // First certification
        Cell cert1 = certRow.createCell(0);
        cert1.setCellValue(
                "   1. I HEREBY CERTIFY on my official oath to the correctness of the above roll.\n" +
                        "   Payment is hereby approved from the appropriation indicated.");
        cert1.setCellStyle(certStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, cert1Width - 1));

        // Second certification
        Cell cert2 = certRow.createCell(cert1Width);
        cert2.setCellValue("    2. APPROVED");
        cert2.setCellStyle(certStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, cert1Width, cert1Width + cert2Width - 1));

        // Third certification
        Cell cert3 = certRow.createCell(cert1Width + cert2Width);
        cert3.setCellValue(
                "   3. I HEREBY CERTIFY on my official oath that I have this ___ day of ____ paid in cash to each man whose name appears on the above roll, the amount set opposite his name, he having presented himself, established his identity and affixed his signature or thumbmark on the space provided therefor.");
        cert3.setCellStyle(certStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, cert1Width + cert2Width, totalColumns));

        // Create signature rows with proper spacing
        Row spacerRow = sheet.createRow(startRow + 1);
        spacerRow.setHeight((short) (20 * 20));

        Row nameRow = sheet.createRow(startRow + 2);
        Row roleRow = sheet.createRow(startRow + 3);
        nameRow.setHeight((short) (25 * 20));
        roleRow.setHeight((short) (20 * 20));

        // First signature block
        Cell name1 = nameRow.createCell(0);
        Cell role1 = roleRow.createCell(0);
        name1.setCellValue("JESCYN KATE N. RAMOS");
        role1.setCellValue("E2P - ICT Coordinator");
        name1.setCellStyle(nameStyle);
        role1.setCellStyle(roleStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 2, startRow + 2, 0, cert1Width - 1));
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, 0, cert1Width - 1));

        // Second signature block
        Cell name2 = nameRow.createCell(cert1Width);
        Cell role2 = roleRow.createCell(cert1Width);
        name2.setCellValue("CARLOS JERICHO L. PETILLA");
        role2.setCellValue("Provincial Governor");
        name2.setCellStyle(nameStyle);
        role2.setCellStyle(roleStyle);
        sheet.addMergedRegion(
                new CellRangeAddress(startRow + 2, startRow + 2, cert1Width, cert1Width + cert2Width - 1));
        sheet.addMergedRegion(
                new CellRangeAddress(startRow + 3, startRow + 3, cert1Width, cert1Width + cert2Width - 1));

        // Third signature block
        Cell name3 = nameRow.createCell(cert1Width + cert2Width);
        Cell role3 = roleRow.createCell(cert1Width + cert2Width);
        name3.setCellValue("JOAN FLORLYN JUSTISA");
        role3.setCellValue("E2P - ICT");
        name3.setCellStyle(nameStyle);
        role3.setCellStyle(roleStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 2, startRow + 2, cert1Width + cert2Width, totalColumns));
        sheet.addMergedRegion(new CellRangeAddress(startRow + 3, startRow + 3, cert1Width + cert2Width, totalColumns));

        // Add spacing before note
        Row noteSpacerRow = sheet.createRow(startRow + 4);
        noteSpacerRow.setHeight((short) (20 * 20));

        // Note section
        Row noteRow = sheet.createRow(startRow + 5);
        noteRow.setHeight((short) (40 * 20));
        Cell note = noteRow.createCell(0);
        note.setCellValue(
                "   *NOTE: Where thumbmark is to be used in place of signature, and the space available is not sufficient, the thumbmark may be impressed on the back hereof with proper indication of the corresponding student's number and on the corresponding line on the payroll\n"
                        +
                        "   \u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0or remark 'see thumbmark on the back' should be written.");
        CellStyle noteStyle = sheet.getWorkbook().createCellStyle();
        noteStyle.cloneStyleFrom(certStyle);
        noteStyle.setWrapText(true);
        noteStyle.setAlignment(HorizontalAlignment.LEFT);
        noteStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        note.setCellStyle(noteStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 5, startRow + 5, 0, totalColumns));

        // Tagline section
        Row taglineRow = sheet.createRow(startRow + 6);
        taglineRow.setHeight((short) (35 * 20));
        Cell tagline = taglineRow.createCell(0);
        tagline.setCellValue("\"IPAKITA SA MUNDO, UMAASENSO NA TAYO\"");
        CellStyle taglineStyle = sheet.getWorkbook().createCellStyle();
        Font taglineFont = sheet.getWorkbook().createFont();
        taglineFont.setBold(true);
        taglineFont.setFontHeightInPoints((short) 16);
        taglineStyle.setFont(taglineFont);
        taglineStyle.setAlignment(HorizontalAlignment.CENTER);
        taglineStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        tagline.setCellStyle(taglineStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 6, startRow + 6, 0, totalColumns));
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
     * Adds borders to a cell style.
     *
     * @param style The style to modify.
     */
    private void addBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
    }
}