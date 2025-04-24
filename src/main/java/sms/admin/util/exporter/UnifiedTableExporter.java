package sms.admin.util.exporter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Paragraph;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.beans.value.ObservableValue;
import javafx.util.Callback;
import javafx.scene.control.TableCell;

public class UnifiedTableExporter<T> {
    protected static final String PESO = "₱";
    private final Map<Integer, Double> consolidatedData = new HashMap<>();
    private double multiplier = 1.0;

    // Style properties
    protected CellStyle headerStyle;
    protected CellStyle dataStyle;
    protected CellStyle currencyStyle;

    public void setMultiplier(double value) {
        this.multiplier = value;
    }

    public void setConsolidatedValue(int id, double value) {
        consolidatedData.put(id, value);
    }

    public void exportToExcel(TableView<T> table, String title, String outputPath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(title);
            initializeStyles(workbook);
            writeExcelContent(sheet, table, title);

            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            }
        }
    }

    public void exportToPdf(TableView<T> table, String title, String outputPath) throws IOException {
        try (PdfWriter writer = new PdfWriter(outputPath);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf)) {

            writePdfContent(document, table, title);
        }
    }

    public void exportToCsv(TableView<T> table, String title, String outputPath) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(outputPath), StandardCharsets.UTF_8)) {
            writeCsvContent(writer, table, title);
        }
    }

    protected void initializeStyles(Workbook workbook) {
        headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        dataStyle = workbook.createCellStyle();

        currencyStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        currencyStyle.setDataFormat(format.getFormat(PESO + "#,##0.00"));
    }

    protected void writeExcelContent(Sheet sheet, TableView<T> table, String title) {
        int rowNum = 0;

        // Write title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(headerStyle);

        // Write headers
        Row headerRow = sheet.createRow(rowNum++);
        List<TableColumn<T, ?>> columns = table.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns.get(i).getText());
            cell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(i);
        }

        // Write data
        for (T item : table.getItems()) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = row.createCell(i);
                String value = getFormattedCellValue(item, columns.get(i));
                cell.setCellValue(value);
                cell.setCellStyle(dataStyle);
            }
        }
    }

    protected void writePdfContent(Document document, TableView<T> table, String title) {
        document.add(new Paragraph(title));

        List<TableColumn<T, ?>> columns = table.getColumns();
        Table pdfTable = new Table(columns.size());

        // Add headers
        for (TableColumn<T, ?> column : columns) {
            pdfTable.addCell(column.getText());
        }

        // Add data
        for (T item : table.getItems()) {
            for (TableColumn<T, ?> column : columns) {
                pdfTable.addCell(getFormattedCellValue(item, column));
            }
        }

        document.add(pdfTable);
    }

    protected void writeCsvContent(Writer writer, TableView<T> table, String title) throws IOException {
        List<TableColumn<T, ?>> columns = table.getColumns();

        // Write headers
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0)
                writer.write(",");
            writer.write(escapeCsv(columns.get(i).getText()));
        }
        writer.write("\n");

        // Write data
        for (T item : table.getItems()) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0)
                    writer.write(",");
                writer.write(escapeCsv(getFormattedCellValue(item, columns.get(i))));
            }
            writer.write("\n");
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected String getFormattedCellValue(Object item, TableColumn<?, ?> column) {
        try {
            // Get value using cell value factory
            if (column.getCellValueFactory() != null) {
                ObservableValue observable = column.getCellValueFactory()
                        .call(new TableColumn.CellDataFeatures(null, column, item));

                if (observable != null) {
                    Object value = observable.getValue();

                    // Format using cell factory if available
                    if (value != null && column.getCellFactory() != null) {
                        Callback<TableColumn, TableCell> cellFactory = (Callback<TableColumn, TableCell>) (Object) column
                                .getCellFactory();

                        TableCell cell = cellFactory.call(column);
                        if (cell != null) {
                            cell.setItem(value);
                            String text = cell.getText();
                            if (text != null && !text.isEmpty()) {
                                return text;
                            }
                        }
                        return value.toString();
                    }
                    return value != null ? value.toString() : "";
                }
            }
            return item != null ? item.toString() : "";

        } catch (Exception e) {
            System.err.println("Error formatting cell value: " + e.getMessage());
            return "";
        }
    }

    protected String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
