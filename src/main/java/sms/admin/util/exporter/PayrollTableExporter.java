package sms.admin.util.exporter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import dev.finalproject.models.Student;
import javafx.scene.control.TableView;

public class PayrollTableExporter {
    private double fareMultiplier = 1.0;
    private final Map<Integer, Double> consolidatedDays = new HashMap<>();

    public void setFareMultiplier(double multiplier) {
        this.fareMultiplier = multiplier;
    }

    public void setConsolidatedDays(int studentId, double days) {
        consolidatedDays.put(studentId, days);
    }

    public void exportToExcel(TableView<Student> table, String title, String outputPath, List<Student> consolidatedData) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(title);
            
            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("₱#,##0.00"));

            // Create header row
            Row headerRow = sheet.createRow(0);
            createExcelHeaderCell(headerRow, 0, "Student ID", headerStyle);
            createExcelHeaderCell(headerRow, 1, "Full Name", headerStyle);
            createExcelHeaderCell(headerRow, 2, "Total Days", headerStyle);
            createExcelHeaderCell(headerRow, 3, "Fare", headerStyle);
            createExcelHeaderCell(headerRow, 4, "Total Amount", headerStyle);

            // Create data rows
            int rowNum = 1;
            double totalAmount = 0;
            for (Student student : consolidatedData) {
                Row row = sheet.createRow(rowNum++);
                createExcelCell(row, 0, String.valueOf(student.getStudentID()));
                createExcelCell(row, 1, formatFullName(student));
                
                // Get consolidated days
                double totalDays = consolidatedDays.getOrDefault(student.getStudentID(), 0.0);
                createExcelCell(row, 2, String.format("%.1f day(s)", totalDays));
                
                double fare = student.getFare() * fareMultiplier;
                org.apache.poi.ss.usermodel.Cell fareCell = row.createCell(3);
                fareCell.setCellValue(fare);
                fareCell.setCellStyle(currencyStyle);
                
                double amount = totalDays * fare;
                totalAmount += amount;
                org.apache.poi.ss.usermodel.Cell amountCell = row.createCell(4);
                amountCell.setCellValue(amount);
                amountCell.setCellStyle(currencyStyle);
            }

            // Add total row
            Row totalRow = sheet.createRow(rowNum);
            createExcelCell(totalRow, 3, "Total Amount:");
            org.apache.poi.ss.usermodel.Cell totalAmountCell = totalRow.createCell(4);
            totalAmountCell.setCellValue(totalAmount);
            totalAmountCell.setCellStyle(currencyStyle);

            // Auto-size columns
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            }
        }
    }

    private void createExcelHeaderCell(Row row, int column, String value, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createExcelCell(Row row, int column, String value) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(column);
        cell.setCellValue(value);
    }

    public void exportToPdf(TableView<Student> table, String title, String outputPath, List<Student> consolidatedData) throws IOException {
        try (PdfWriter writer = new PdfWriter(outputPath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Add title
            document.add(new Paragraph(title)
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

            // Create table with 5 columns
            Table pdfTable = new Table(UnitValue.createPercentArray(new float[]{15, 35, 15, 15, 20}))
                .useAllAvailableWidth();

            // Add header row
            pdfTable.addCell(createPdfHeaderCell("Student ID"));
            pdfTable.addCell(createPdfHeaderCell("Full Name"));
            pdfTable.addCell(createPdfHeaderCell("Total Days"));
            pdfTable.addCell(createPdfHeaderCell("Fare"));
            pdfTable.addCell(createPdfHeaderCell("Total Amount"));

            // Add data rows
            double totalAmount = 0;
            for (Student student : consolidatedData) {
                double totalDays = consolidatedDays.getOrDefault(student.getStudentID(), 0.0);
                double fare = student.getFare() * fareMultiplier;
                double amount = totalDays * fare;
                totalAmount += amount;

                pdfTable.addCell(createPdfCell(String.valueOf(student.getStudentID())));
                pdfTable.addCell(createPdfCell(formatFullName(student)));
                pdfTable.addCell(createPdfCell(String.format("%.1f day(s)", totalDays)));
                pdfTable.addCell(createPdfCell(String.format("₱%.2f", fare)));
                pdfTable.addCell(createPdfCell(String.format("₱%.2f", amount)));
            }

            // Add total row
            for (int i = 0; i < 3; i++) {
                pdfTable.addCell(createPdfCell(""));
            }
            pdfTable.addCell(createPdfHeaderCell("Total Amount:"));
            pdfTable.addCell(createPdfHeaderCell(String.format("₱%.2f", totalAmount)));

            document.add(pdfTable);
        }
    }

    public void exportToCsv(TableView<Student> table, String title, String outputPath, List<Student> consolidatedData) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8)) {
            // Write header
            writer.write("Student ID,Full Name,Total Days,Fare,Total Amount\n");

            // Write data rows
            double totalAmount = 0;
            for (Student student : consolidatedData) {
                double totalDays = consolidatedDays.getOrDefault(student.getStudentID(), 0.0);
                double fare = student.getFare() * fareMultiplier;
                double amount = totalDays * fare;
                totalAmount += amount;

                writer.write(String.format("%d,\"%s\",%.1f,₱%.2f,₱%.2f\n",
                    student.getStudentID(),
                    formatFullName(student),
                    totalDays,
                    fare,
                    amount));
            }

            // Write total
            writer.write(String.format(",,,,₱%.2f\n", totalAmount));
        }
    }

    private Cell createPdfHeaderCell(String text) {
        return new Cell()
            .add(new Paragraph(text))
            .setBold()
            .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell createPdfCell(String text) {
        return new Cell()
            .add(new Paragraph(text))
            .setTextAlignment(TextAlignment.CENTER);
    }

    private String formatFullName(Student student) {
        return String.format("%s, %s %s",
            student.getLastName(),
            student.getFirstName(),
            student.getMiddleName());
    }
}
