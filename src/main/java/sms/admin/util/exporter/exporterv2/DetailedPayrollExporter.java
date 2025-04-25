package sms.admin.util.exporter.exporterv2;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
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
    private static final String TEMPLATE_PATH = "/templates/Payroll Template.xlsx";

    // Instance variables
    private final YearMonth month;
    private final ObservableList<AttendanceLog> attendanceLogs;
    private double fareMultiplier = 1.0;
    private int totalDaysColumn;
    private int totalAmountColumn;
    private boolean isNewWorkbook = false;

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

    /**
     * Exports the payroll data to an Excel file using a template.
     *
     * @param table      The TableView containing student data.
     * @param title      The title of the sheet.
     * @param outputPath The path to save the Excel file.
     * @throws Exception If an error occurs during export.
     */
    public void exportToExcel(TableView<Student> table, String title, String outputPath) throws Exception {
        final Workbook workbook;
        final Sheet sheet;

        // Try to load template first
        try (InputStream templateStream = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (templateStream != null) {
                workbook = new XSSFWorkbook(templateStream);
                sheet = workbook.getSheetAt(0);
                isNewWorkbook = false;
            } else {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet(title);
                isNewWorkbook = true;
            }
        }

        try (workbook) {
            // Create cell styles
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Calculate weeks
            List<List<LocalDate>> weeks = getWorkWeeks();
            int totalDays = weeks.stream().mapToInt(List::size).sum();

            // Create headers if this is a new sheet
            createTableHeaders(sheet, 3, headerStyle, centerStyle, weeks);

            // Write student data rows
            int rowNum = 7;
            double grandTotal = 0;
            int noCounter = 1;

            for (Student student : table.getItems()) {
                Row row = sheet.createRow(rowNum++);
                row.setHeight((short) (25 * 20));
                grandTotal += writeStudentRow(row, student, weeks, dataStyle,
                        currencyStyle, centerStyle, noCounter++);
            }

            // Update subtotal
            // Row subtotalRow = sheet.createRow(rowNum++);
            // subtotalRow.setHeight((short) (25 * 20));
            // createSubtotalRow(subtotalRow, grandTotal, workbook);

            // Write to output file
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            }
        }
    }

    /**
     * Updates the certification section dates.
     */
    private void updateCertificationDates(Sheet sheet, int startRow) {
        Row certRow = sheet.getRow(startRow);
        if (certRow != null) {
            Cell cert3 = certRow.getCell(totalAmountColumn - 2);
            if (cert3 != null) {
                LocalDate now = LocalDate.now();
                String certText = cert3.getStringCellValue()
                        .replace("[DAY]", String.valueOf(now.getDayOfMonth()))
                        .replace("[MONTH]", now.getMonth().toString());
                cert3.setCellValue(certText);
            }
        }
    }

    /**
     * Retrieves work weeks for the specified month, excluding weekends.
     *
     * @return A list of lists, where each inner list represents a week of working
     *         days.
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
                .map(CommonAttendanceUtil::computeAttendanceStatus)
                .findFirst()
                .orElse(CommonAttendanceUtil.ABSENT_MARK);
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
     * @param sheet       The sheet to modify.
     * @param startRow    The starting row for headers.
     * @param headerStyle Style for header cells.
     * @param centerStyle Style for centered text.
     * @param weeks       List of work weeks.
     * @return The total number of days.
     */
    private int createTableHeaders(Sheet sheet, int startRow, CellStyle headerStyle,
            CellStyle centerStyle, List<List<LocalDate>> weeks) {
        if (!isNewWorkbook) {
            return weeks.stream().mapToInt(List::size).sum();
        }

        // First clear any existing merged regions to avoid conflicts
        while (sheet.getNumMergedRegions() > 0) {
            sheet.removeMergedRegion(0);
        }

        // Create rows
        Row headerRow1 = sheet.createRow(startRow);
        Row headerRow2 = sheet.createRow(startRow + 1);
        Row headerRow3 = sheet.createRow(startRow + 2);
        Row headerRow4 = sheet.createRow(startRow + 3);

        // Set row heights
        headerRow1.setHeight((short) (30 * 20));
        headerRow2.setHeight((short) (25 * 20));
        headerRow3.setHeight((short) (25 * 20));
        headerRow4.setHeight((short) (25 * 20));

        // No. and Name headers
        Cell noHeader = headerRow1.createCell(0);
        noHeader.setCellValue("No.");
        noHeader.setCellStyle(headerStyle);
        addMergedRegionIfNotExists(sheet, new CellRangeAddress(startRow, startRow + 3, 0, 0));

        Cell nameHeader = headerRow1.createCell(1);
        nameHeader.setCellValue("Student Name");
        nameHeader.setCellStyle(headerStyle);
        addMergedRegionIfNotExists(sheet, new CellRangeAddress(startRow, startRow + 3, 1, 1));

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
        addMergedRegionIfNotExists(sheet,
                new CellRangeAddress(startRow, startRow, currentCol, currentCol + totalDays - 1));

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
                    addMergedRegionIfNotExists(sheet, new CellRangeAddress(startRow + 1, startRow + 1, weekStartCol,
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

        // Calculate total days for return value
        return weeks.stream().mapToInt(List::size).sum();
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
        if (!isNewWorkbook) {
            return;
        }
        CellStyle subHeaderStyle = createSubHeaderStyle(sheet.getWorkbook());
        int currentCol = startCol;

        Cell totalDaysHeader = sheet.getRow(startRow).createCell(currentCol);
        totalDaysHeader.setCellValue("Total Days");
        totalDaysHeader.setCellStyle(headerStyle);
        addMergedRegionIfNotExists(sheet, new CellRangeAddress(startRow, startRow + 3, currentCol, currentCol));
        currentCol++;

        Cell transHeader = sheet.getRow(startRow).createCell(currentCol);
        transHeader.setCellValue("Transportation Allowance");
        transHeader.setCellStyle(headerStyle);
        addMergedRegionIfNotExists(sheet, new CellRangeAddress(startRow, startRow, currentCol, currentCol + 1));

        Cell transRateHeader = sheet.getRow(startRow + 1).createCell(currentCol);
        transRateHeader.setCellValue("Daily Rate");
        transRateHeader.setCellStyle(subHeaderStyle);
        addMergedRegionIfNotExists(sheet, new CellRangeAddress(startRow + 1, startRow + 3, currentCol, currentCol));

        Cell transAmountHeader = sheet.getRow(startRow + 1).createCell(currentCol + 1);
        transAmountHeader.setCellValue("Amount Due");
        transAmountHeader.setCellStyle(subHeaderStyle);
        addMergedRegionIfNotExists(sheet,
                new CellRangeAddress(startRow + 1, startRow + 3, currentCol + 1, currentCol + 1));
        currentCol += 2;

        Cell mealHeader = sheet.getRow(startRow).createCell(currentCol);
        mealHeader.setCellValue("Meal Allowance");
        mealHeader.setCellStyle(headerStyle);
        addMergedRegionIfNotExists(sheet, new CellRangeAddress(startRow, startRow, currentCol, currentCol + 1));

        Cell mealRateHeader = sheet.getRow(startRow + 1).createCell(currentCol);
        mealRateHeader.setCellValue("Daily Rate");
        mealRateHeader.setCellStyle(subHeaderStyle);
        addMergedRegionIfNotExists(sheet, new CellRangeAddress(startRow + 1, startRow + 3, currentCol, currentCol));

        Cell mealAmountHeader = sheet.getRow(startRow + 1).createCell(currentCol + 1);
        mealAmountHeader.setCellValue("Amount Due");
        mealAmountHeader.setCellStyle(subHeaderStyle);
        addMergedRegionIfNotExists(sheet,
                new CellRangeAddress(startRow + 1, startRow + 3, currentCol + 1, currentCol + 1));
        currentCol += 2;

        Cell totalAmountHeader = sheet.getRow(startRow).createCell(totalAmountColumn);
        totalAmountHeader.setCellValue("Total Amount Received");
        totalAmountHeader.setCellStyle(headerStyle);
        addMergedRegionIfNotExists(sheet,
                new CellRangeAddress(startRow, startRow + 3, totalAmountColumn, totalAmountColumn));
        currentCol = totalAmountColumn + 1;

        Cell signNoHeader = sheet.getRow(startRow).createCell(currentCol);
        signNoHeader.setCellValue("No.");
        signNoHeader.setCellStyle(headerStyle);
        addMergedRegionIfNotExists(sheet, new CellRangeAddress(startRow, startRow + 3, currentCol, currentCol));
        currentCol++;

        Cell signatureHeader = sheet.getRow(startRow).createCell(currentCol);
        signatureHeader.setCellValue("Signature");
        signatureHeader.setCellStyle(headerStyle);
        addMergedRegionIfNotExists(sheet, new CellRangeAddress(startRow, startRow + 3, currentCol, currentCol));
    }

    private void addMergedRegionIfNotExists(Sheet sheet, CellRangeAddress newRegion) {
        // Check for any overlapping regions
        boolean hasOverlap = false;
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress existing = sheet.getMergedRegion(i);
            if (overlaps(existing, newRegion)) {
                hasOverlap = true;
                break;
            }
        }

        if (!hasOverlap) {
            sheet.addMergedRegion(newRegion);
        }
    }

    private boolean overlaps(CellRangeAddress range1, CellRangeAddress range2) {
        return !(range1.getLastRow() < range2.getFirstRow() ||
                range1.getFirstRow() > range2.getLastRow() ||
                range1.getLastColumn() < range2.getFirstColumn() ||
                range1.getFirstColumn() > range2.getLastColumn());
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
                    case CommonAttendanceUtil.PRESENT_MARK,
                            CommonAttendanceUtil.EXCUSED_MARK,
                            CommonAttendanceUtil.HOLIDAY_MARK ->
                        totalDays++;
                    case CommonAttendanceUtil.HALF_DAY_MARK -> totalDays += 0.5;
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
                    case CommonAttendanceUtil.PRESENT_MARK,
                            CommonAttendanceUtil.EXCUSED_MARK,
                            CommonAttendanceUtil.HOLIDAY_MARK ->
                        totalDays += 1.0;
                    case CommonAttendanceUtil.HALF_DAY_MARK -> totalDays += 0.5;
                }
            }
            return totalDays;
        } catch (Exception e) {
            System.err.println("Error calculating days for student " + student.getStudentID());
            e.printStackTrace();
            return 0;
        }
    }
}