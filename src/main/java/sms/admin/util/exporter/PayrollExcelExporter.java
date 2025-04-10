package sms.admin.util.exporter;

import javafx.collections.ObservableList;
import dev.finalproject.models.Student;

import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PayrollExcelExporter {

    public Workbook createWorkbook() {
        return new XSSFWorkbook();
    }

    public CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public CellStyle createCenterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    public CellStyle createBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    public void exportToExcel(String filePath) {
        try (Workbook workbook = createWorkbook()) {
            Sheet sheet = workbook.createSheet("Payroll");
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = { "Employee ID", "Name", "Position", "Salary" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add data rows here...

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeDataToWorkbook(Workbook workbook, ObservableList<Student> items, String title) {
        Sheet sheet = workbook.createSheet("Payroll");

        // Create header row with payment details
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue(title);

        // Create column headers
        Row columnRow = sheet.createRow(1);
        String[] headers = { "Student ID", "Name", "Transportation Rate", "Meal Rate", "Total Days", "Total Amount" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = columnRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
            sheet.setColumnWidth(i, 4000); // Set reasonable column widths
        }

        // Populate data rows
        int rowNum = 2;
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle centerStyle = createCenterStyle(workbook);

        for (Student student : items) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(student.getStudentID()); // Student ID
            row.createCell(1).setCellValue(student.getFullName()); // Full Name

            Cell fareCell = row.createCell(2);
            fareCell.setCellValue(student.getFare());
            fareCell.setCellStyle(currencyStyle);

            Cell mealCell = row.createCell(3);
            mealCell.setCellValue(1300.00); // Standard meal rate
            mealCell.setCellStyle(currencyStyle);

            // These values should be calculated based on attendance
            row.createCell(4).setCellValue(0); // Total Days - placeholder

            Cell totalCell = row.createCell(5);
            totalCell.setCellValue(0.0); // Total Amount - placeholder
            totalCell.setCellStyle(currencyStyle);
        }
    }

    public String getSheetName() {
        return "Payroll";
    }
}