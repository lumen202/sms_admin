package sms.admin.util.exporter;

import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Paragraph;
import java.io.*;
import java.util.List;

public abstract class BaseTableExporter<T> implements TableDataProvider<T> {

    public void exportToExcel(TableView<T> table, String title, String outputPath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            ObservableList<T> items = table.getItems();
            writeDataToWorkbook(workbook, items, title);
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportToPdf(TableView<T> table, String title, String outputPath) {
        try {
            PdfWriter writer = new PdfWriter(outputPath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            writeDataToPdf(document, table.getItems(), title);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportToCsv(TableView<T> table, String title, String outputPath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writeDataToCsv(writer, table.getItems(), title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void writeDataToWorkbook(Workbook workbook, ObservableList<T> items, String title) {
        Sheet sheet = workbook.createSheet(getSheetName());
        writeBasicSheet(workbook, sheet, items, title);
    }

    protected void writeDataToPdf(Document document, ObservableList<T> items, String title) {
        writeBasicPdf(document, items, title);
    }

    protected void writeDataToCsv(PrintWriter writer, ObservableList<T> items, String title) {
        writeBasicCsv(writer, items, title);
    }

    protected String escapeCsvField(String field) {
        if (field == null)
            return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    protected void writeBasicSheet(Workbook workbook, Sheet sheet, ObservableList<T> items, String title) {
        int rowNum = 0;

        // Write title
        Row titleRow = sheet.createRow(rowNum++);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);

        // Write headers
        Row headerRow = sheet.createRow(rowNum++);
        List<String> headers = getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
        }

        // Write data
        for (T item : items) {
            Row row = sheet.createRow(rowNum++);
            List<String> rowData = getRowData(item);
            for (int i = 0; i < rowData.size(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = row.createCell(i);
                cell.setCellValue(rowData.get(i));
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    protected void writeBasicPdf(Document document, ObservableList<T> items, String title) {
        document.add(new Paragraph(title));

        List<String> headers = getHeaders();
        Table table = new Table(headers.size());

        // Add headers
        for (String header : headers) {
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)));
        }

        // Add data
        for (T item : items) {
            List<String> rowData = getRowData(item);
            for (String value : rowData) {
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(value)));
            }
        }

        document.add(table);
    }

    protected void writeBasicCsv(PrintWriter writer, ObservableList<T> items, String title) {
        writer.println(title);
        writer.println();

        // Write headers
        List<String> headers = getHeaders();
        writer.println(String.join(",", headers.stream().map(this::escapeCsvField).toList()));

        // Write data
        for (T item : items) {
            List<String> rowData = getRowData(item);
            writer.println(String.join(",", rowData.stream().map(this::escapeCsvField).toList()));
        }
    }
}
