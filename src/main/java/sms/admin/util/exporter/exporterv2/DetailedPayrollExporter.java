package sms.admin.util.exporter.exporterv2;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private static final double MAX_TOTAL_AMOUNT = 1300.00;

    // Instance variables
    private final YearMonth month;
    private final ObservableList<AttendanceLog> attendanceLogs;
    private double fareMultiplier = 1.0;
    private int totalDaysColumn;
    private int totalAmountColumn;

    // Constructor
    public DetailedPayrollExporter(YearMonth month, YearMonth endMonth, ObservableList<AttendanceLog> logs) {
        this.month = month;
        this.attendanceLogs = logs;
    }

    // Setter for fare multiplier
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
            CellStyle centerStyle = createCenterStyle(workbook);

            // Create a new style for subtotal label (left-aligned)
            CellStyle subtotalLabelStyle = workbook.createCellStyle();
            Font subtotalFont = workbook.createFont();
            subtotalFont.setBold(true);
            subtotalFont.setFontHeightInPoints((short) 14);
            subtotalLabelStyle.setFont(subtotalFont);
            subtotalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
            subtotalLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            addBorders(subtotalLabelStyle);

            // Set initial column widths with consistent small values
            sheet.setColumnWidth(0, 5 * 256); // No. column
            sheet.setColumnWidth(1, 40 * 256); // Name column

            // Get weeks and total days
            List<List<LocalDate>> weeks = getWorkWeeks();
            int totalDays = weeks.stream().mapToInt(List::size).sum();

            // Set ALL day columns to exactly the same small width
            final int DAY_COLUMN_WIDTH = 3 * 256; // Very small width for day columns
            for (int i = 2; i < 2 + totalDays; i++) {
                sheet.setColumnWidth(i, DAY_COLUMN_WIDTH);
            }

            // Set other columns with consistent values
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

            // Increase row heights
            short normalRowHeight = (short) (25 * 20);
            short headerRowHeight = (short) (30 * 20);
            short titleRowHeight = (short) (35 * 20);

            // Form number and title layout
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
            mainTitleFont.setFontHeightInPoints((short) 14);
            mainTitleStyle.setFont(mainTitleFont);
            mainTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            mainTitleCell.setCellStyle(mainTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 8, 26));

            // Period description
            Row periodRow = sheet.createRow(1);
            periodRow.setHeight(headerRowHeight);
            Cell periodCell = periodRow.createCell(0);
            CellStyle periodStyle = workbook.createCellStyle();
            Font periodFont = workbook.createFont();
            periodFont.setBold(true);
            periodFont.setFontHeightInPoints((short) 13);
            periodStyle.setFont(periodFont);
            periodStyle.setAlignment(HorizontalAlignment.CENTER);
            periodStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            periodCell.setCellValue(
                    "For labor on _________ - Baybay Data Center, at Baybay National High School, Baybay City, Leyte, Philippines, for the period,    "
                            + month.getMonth().toString() + " " + month.getYear());

            periodCell.setCellStyle(periodStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 34));

            sheet.createRow(2).setHeight((short) (15 * 20));

            // Store column indices
            totalDaysColumn = totalDaysCol;
            totalAmountColumn = totalAmountCol;

            // Create table headers
            totalDays = createTableHeaders(sheet, 3, headerStyle, centerStyle, weeks);
            int totalAmountColCalculated = 2 + totalDays + 5;

            // Write data rows
            int rowNum = 7;
            double grandTotal = 0;
            int noCounter = 1;
            for (Student student : table.getItems()) {
                Row row = sheet.createRow(rowNum++);
                row.setHeight(normalRowHeight);
                grandTotal += writeStudentRow(row, student, weeks, dataStyle, currencyStyle, centerStyle, noCounter++);
            }

            // Create subtotal row with new style
            Row subtotalRow = sheet.createRow(rowNum++);
            subtotalRow.setHeight(normalRowHeight);
            Cell subtotalLabel = subtotalRow.createCell(0);
            subtotalLabel.setCellValue("SUB - TOTAL FOR THIS PAGE:  ");
            subtotalLabel.setCellStyle(subtotalLabelStyle); // Use left-aligned style
            sheet.addMergedRegion(new CellRangeAddress(
                    subtotalRow.getRowNum(),
                    subtotalRow.getRowNum(),
                    0,
                    totalAmountCol - 1));
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

    // Optimized method to get work weeks
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

    // Data processing methods
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

    private String formatFullName(Student student) {
        String lastName = capitalizeWord(student.getLastName());
        String firstName = capitalizeWord(student.getFirstName());
        String middleName = student.getMiddleName() != null ? capitalizeWord(student.getMiddleName()) + " " : "";
        String extension = student.getNameExtension() != null ? " " + student.getNameExtension().toUpperCase() : "";
        return String.format("%s, %s %s%s", lastName, firstName, middleName, extension);
    }

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

    private String formatDayName(LocalDate date) {
        return CommonAttendanceUtil.getDayInitial(date.getDayOfWeek());
    }

    // Excel sheet building methods
    private int createTableHeaders(Sheet sheet, int startRow, CellStyle headerStyle,
            CellStyle centerStyle, List<List<LocalDate>> weeks) {
        // Create header rows with reduced height
        Row headerRow1 = sheet.createRow(startRow);
        Row headerRow2 = sheet.createRow(startRow + 1);
        Row headerRow3 = sheet.createRow(startRow + 2);
        Row headerRow4 = sheet.createRow(startRow + 3);

        // Basic column headers (No. and Name)
        Cell noHeader = headerRow1.createCell(0);
        noHeader.setCellValue("No.");
        noHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, 0, 0));

        Cell nameHeader = headerRow1.createCell(1);
        nameHeader.setCellValue("Student Name");
        nameHeader.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + 3, 1, 1));

        // Time roll section setup
        int currentCol = 2;
        Cell timeRollHeader = headerRow1.createCell(currentCol);
        timeRollHeader.setCellValue("TIME ROLL");
        timeRollHeader.setCellStyle(headerStyle);

        // Calculate total days and max day digits for width
        int totalDays = weeks.stream().mapToInt(List::size).sum();
        int maxDayDigits = String.valueOf(month.lengthOfMonth()).length();
        final int TIME_ROLL_WIDTH = (maxDayDigits + 2) * 256;

        // Set consistent width for all day columns
        for (int i = 0; i < totalDays; i++) {
            sheet.setColumnWidth(currentCol + i, TIME_ROLL_WIDTH);
        }

        // Merge TIME ROLL header
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, currentCol, currentCol + totalDays - 1));

        // Process weeks consistently
        int weekStartCol = currentCol;
        int weekNum = 1;
        for (List<LocalDate> week : weeks) {
            if (!week.isEmpty()) {
                int daysInWeek = week.size();

                // Week header
                Cell weekHeader = headerRow2.createCell(weekStartCol);
                weekHeader.setCellValue("Week " + weekNum++);
                weekHeader.setCellStyle(centerStyle);

                // Merge week header if multiple days
                if (daysInWeek > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(
                            startRow + 1,
                            startRow + 1,
                            weekStartCol,
                            weekStartCol + daysInWeek - 1));
                }

                // Add days for this week
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

        // Rest of the columns
        int remainingCol = weekStartCol;
        sheet.setColumnWidth(remainingCol, 6 * 256); // Total Days

        // Set larger widths for allowance columns
        int allowanceWidth = 9 * 256; // Increased width for allowance columns

        sheet.setColumnWidth(remainingCol + 1, allowanceWidth); // Trans Daily Rate
        sheet.setColumnWidth(remainingCol + 2, allowanceWidth); // Trans Amount Due
        sheet.setColumnWidth(remainingCol + 3, allowanceWidth); // Meal Daily Rate
        sheet.setColumnWidth(remainingCol + 4, allowanceWidth); // Meal Amount Due

        // Merge regions should match the new widths
        sheet.setColumnWidth(remainingCol + 5, 10 * 256); // Total Amount
        sheet.setColumnWidth(remainingCol + 6, 4 * 256); // No. for signature
        sheet.setColumnWidth(remainingCol + 7, 20 * 256); // Signature

        // Continue with remaining headers
        currentCol = weekStartCol;
        addRemainingHeaders(sheet, startRow, currentCol, headerStyle, centerStyle);

        return totalDays;
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

        Cell mealRateHeader = sheet.getRow(startRow + 1).createCell(currentCol);
        mealRateHeader.setCellValue("Daily Rate");
        mealRateHeader.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 3, currentCol, currentCol));

        Cell mealAmountHeader = sheet.getRow(startRow + 1).createCell(currentCol + 1);
        mealAmountHeader.setCellValue("Amount Due");
        mealAmountHeader.setCellStyle(subHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 3, currentCol + 1, currentCol + 1));
        currentCol += 2;

        // Total Amount
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
                    case CommonAttendanceUtil.PRESENT_MARK:
                    case CommonAttendanceUtil.EXCUSED_MARK:
                    case CommonAttendanceUtil.HOLIDAY_MARK:
                        totalDays++;
                        break;
                    case CommonAttendanceUtil.HALF_DAY_MARK:
                        totalDays += 0.5;
                        break;
                }
                attendanceCell.setCellStyle(centerStyle);
            }
        }

        Cell totalDaysCell = row.createCell(colNum++);
        totalDaysCell.setCellValue(totalDays);
        totalDaysCell.setCellStyle(centerStyle);

        double fareRate = student.getFare() * fareMultiplier;
        double fareAmount = fareRate * totalDays;

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

        double totalAmount = Math.min(fareAmount, MAX_TOTAL_AMOUNT);
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

        // Certification section with fixed column ranges
        Row certRow = sheet.createRow(startRow);
        certRow.setHeight((short) (50 * 20));

        Cell cert1 = certRow.createCell(0);
        cert1.setCellValue(
                "1. I HEREBY CERTIFY on my official oath to the correctness of the above roll. Payment is hereby approved from the appropriation indicated.");
        cert1.setCellStyle(certStyle);
        // Reduce merge region to about one-third of totalDaysColumn
        int cert1EndColumn = Math.max(7, totalDaysColumn / 3);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, cert1EndColumn));

        // Calculate end columns for certifications
        int cert2StartColumn = cert1EndColumn + 1;
        int cert2EndColumn = cert2StartColumn + 8; // Give cert2 a fixed width of 8 columns

        // Adjust cert2
        Cell cert2 = certRow.createCell(cert2StartColumn);
        cert2.setCellValue("2. APPROVED");
        cert2.setCellStyle(certStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, cert2StartColumn, cert2EndColumn));

        // Adjust name2/role2 merged regions to match cert2's width
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

        // Adjust cert3 to start after cert2
        Cell cert3 = certRow.createCell(cert2EndColumn + 1);
        cert3.setCellValue(
                "3. I HEREBY CERTIFY on my official oath that I have this ___ day of ____ paid in cash to each man whose name appears on the above roll, the amount set opposite his name, he having presented himself, established his identity and affixed his signature or thumbmark on the space provided therefor.");
        cert3.setCellStyle(certStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, cert2EndColumn + 1, totalAmountColumn + 2));

        Cell name3 = nameRow.createCell(totalAmountColumn);
        Cell role3 = roleRow.createCell(totalAmountColumn);
        name3.setCellValue("JOAN FLORLYN JUSTISA");
        role3.setCellValue("E2P - ICT");
        name3.setCellStyle(nameStyle);
        role3.setCellStyle(roleStyle);
        sheet.addMergedRegion(
                new CellRangeAddress(startRow + 3, startRow + 3, totalAmountColumn, totalAmountColumn + 2));
        sheet.addMergedRegion(
                new CellRangeAddress(startRow + 4, startRow + 4, totalAmountColumn, totalAmountColumn + 2));

        // Note section
        Row noteRow = sheet.createRow(startRow + 6);
        noteRow.setHeight((short) (30f * 20)); // Increase row height
        Cell note = noteRow.createCell(0);

        // Break text into multiple lines with \n
        note.setCellValue(
                "*NOTE: Where thumbmark is to be used in place of signature, and the space available is not sufficient, the thumbmark may be impressed on the back hereof with proper indication of the corresponding student's number and on the corresponding line on the payroll\n"
                        +
                        "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0or remark 'see thumbmark on the back' should be written.");

        CellStyle noteStyle = sheet.getWorkbook().createCellStyle();
        noteStyle.cloneStyleFrom(certStyle);
        noteStyle.setWrapText(true);
        noteStyle.setAlignment(HorizontalAlignment.LEFT); // Left align for better readability
        noteStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        note.setCellStyle(noteStyle);

        sheet.addMergedRegion(new CellRangeAddress(startRow + 6, startRow + 6, 0, totalAmountColumn + 2));

        // Tagline
        Row taglineRow = sheet.createRow(startRow + 7);
        Cell tagline = taglineRow.createCell(0);
        taglineRow.setHeight((short) (30f * 20)); // Increase row height for tagline
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

    // Cell style creation methods
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
        style.setDataFormat(format.getFormat("#,##0.00"));
        addBorders(style);
        return style;
    }

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

    // Utility method
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