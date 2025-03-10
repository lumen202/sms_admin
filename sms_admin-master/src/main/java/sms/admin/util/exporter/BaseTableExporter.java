package sms.admin.util.exporter;

import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.io.*;

public abstract class BaseTableExporter<T> extends AbstractTableExporter {

    @Override
    public void exportToExcel(TableView<?> tableView, String title, String outputPath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(getSheetName());
            int rowNum = 0;
            
            // Add title
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.createCell(0).setCellValue(title);
            
            @SuppressWarnings("unchecked")
            TableView<T> table = (TableView<T>) tableView;
            
            // Add headers
            Row headerRow = sheet.createRow(rowNum++);
            int colNum = 0;
            for (TableColumn<T, ?> column : table.getColumns()) {
                Cell cell = headerRow.createCell(colNum++);
                cell.setCellValue(column.getText());
            }
            
            // Add data
            for (T item : table.getItems()) {
                Row row = sheet.createRow(rowNum++);
                colNum = 0;
                for (TableColumn<T, ?> column : table.getColumns()) {
                    Cell cell = row.createCell(colNum++);
                    String value = getFormattedCellValue(item, column);
                    formatExcelCell(workbook, cell, value, column);
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < table.getColumns().size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exportToPdf(TableView<?> tableView, String title, String outputPath) {
        try {
            PdfWriter writer = new PdfWriter(outputPath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph(title));

            @SuppressWarnings("unchecked")
            TableView<T> table = (TableView<T>) tableView;
            com.itextpdf.layout.element.Table pdfTable = 
                new com.itextpdf.layout.element.Table(table.getColumns().size());

            // Add headers
            for (TableColumn<T, ?> column : table.getColumns()) {
                pdfTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph(column.getText())));
            }

            // Add data
            for (T item : table.getItems()) {
                for (TableColumn<T, ?> column : table.getColumns()) {
                    String value = getFormattedCellValue(item, column);
                    pdfTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(value)));
                }
            }

            document.add(pdfTable);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exportToCsv(TableView<?> tableView, String title, String outputPath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println(title);
            writer.println();
            
            @SuppressWarnings("unchecked")
            TableView<T> table = (TableView<T>) tableView;
            
            // Write headers
            StringBuilder header = new StringBuilder();
            for (TableColumn<T, ?> column : table.getColumns()) {
                header.append(escapeCsvField(column.getText())).append(",");
            }
            writer.println(header.substring(0, header.length() - 1));

            // Write data
            for (T item : table.getItems()) {
                StringBuilder row = new StringBuilder();
                for (TableColumn<T, ?> column : table.getColumns()) {
                    row.append(escapeCsvField(getFormattedCellValue(item, column))).append(",");
                }
                writer.println(row.substring(0, row.length() - 1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    protected abstract String getSheetName();
    
    protected void formatExcelCell(Workbook workbook, Cell cell, String value, TableColumn<T, ?> column) {
        cell.setCellValue(value);
    }
}
