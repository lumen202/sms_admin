/**
 * Utility class for exporting JavaFX {@link javafx.scene.control.TableView} data
 * to multiple formats: Excel (XLSX), PDF, and CSV.
 * <p>
 * Supports configurable data consolidation and formatting, including currency values.
 * Uses Apache POI for Excel export, iText for PDF creation, and standard I/O for CSV.
 * </p>
 * 
 * @param <T> the type of items contained in the TableView
 */
package sms.admin.util.exporter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Paragraph;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.beans.value.ObservableValue;
import javafx.util.Callback;
import javafx.scene.control.TableCell;

public class UnifiedTableExporter<T> {

    /** Peso currency symbol used for currency formatting. */
    protected static final String PESO = "₱";

    /** Consolidated data map keyed by an integer ID. */
    private final Map<Integer, Double> consolidatedData = new HashMap<>();

    /** Multiplier applied to numeric values when exporting. */
    private double multiplier = 1.0;

    /** Style for header cells in Excel export. */
    protected CellStyle headerStyle;
    /** Style for data cells in Excel export. */
    protected CellStyle dataStyle;
    /** Style for currency cells in Excel export. */
    protected CellStyle currencyStyle;

    /**
     * Sets a multiplier to apply to numeric data values during export.
     *
     * @param value the multiplier factor (e.g., for unit conversion)
     */
    public void setMultiplier(double value) {
        this.multiplier = value;
    }

    /**
     * Stores a consolidated value for a specific identifier.
     *
     * @param id    the ID to associate with the value
     * @param value the value to consolidate
     */
    public void setConsolidatedValue(int id, double value) {
        consolidatedData.put(id, value);
    }

    /**
     * Exports the given TableView data to an Excel (.xlsx) file.
     *
     * @param table      the JavaFX TableView containing data
     * @param title      the sheet name and title to use in the workbook
     * @param outputPath the file path to write the Excel file to
     * @throws IOException if an I/O error occurs during writing
     */
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

    /**
     * Exports the given TableView data to a PDF document.
     *
     * @param table      the JavaFX TableView containing data
     * @param title      the title to include at the top of the PDF
     * @param outputPath the file path to write the PDF to
     * @throws IOException if an I/O error occurs during writing
     */
    public void exportToPdf(TableView<T> table, String title, String outputPath) throws IOException {
        try (PdfWriter writer = new PdfWriter(outputPath);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf)) {

            writePdfContent(document, table, title);
        }
    }

    /**
     * Exports the given TableView data to a CSV file using UTF-8 encoding.
     *
     * @param table      the JavaFX TableView containing data
     * @param title      a title or header to include in the CSV
     * @param outputPath the file path to write the CSV to
     * @throws IOException if an I/O error occurs during writing
     */
    public void exportToCsv(TableView<T> table, String title, String outputPath) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(outputPath), StandardCharsets.UTF_8)) {
            writeCsvContent(writer, table, title);
        }
    }

    /**
     * Initializes common cell styles for Excel export.
     *
     * @param workbook the Workbook in which to create styles
     */
    protected void initializeStyles(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        dataStyle = workbook.createCellStyle();

        currencyStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        currencyStyle.setDataFormat(format.getFormat(PESO + "#,##0.00"));
    }

    /**
     * Writes the content of the TableView into the given Excel sheet.
     *
     * @param sheet the Sheet to populate with data
     * @param table the TableView providing columns and items
     * @param title the title row value
     */
    protected void writeExcelContent(Sheet sheet, TableView<T> table, String title) {
        int rowNum = 0;
        // Title row
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(headerStyle);
        // Header row
        Row headerRow = sheet.createRow(rowNum++);
        List<TableColumn<T, ?>> columns = table.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns.get(i).getText());
            cell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(i);
        }
        // Data rows
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

    /**
     * Writes the content of the TableView into the given PDF document.
     *
     * @param document the PDF Document to add content to
     * @param table    the TableView providing columns and items
     * @param title    the title paragraph text
     */
    protected void writePdfContent(Document document, TableView<T> table, String title) {
        document.add(new Paragraph(title));
        List<TableColumn<T, ?>> columns = table.getColumns();
        Table pdfTable = new Table(columns.size());
        // Headers
        for (TableColumn<T, ?> column : columns) {
            pdfTable.addCell(column.getText());
        }
        // Data
        for (T item : table.getItems()) {
            for (TableColumn<T, ?> column : columns) {
                pdfTable.addCell(getFormattedCellValue(item, column));
            }
        }
        document.add(pdfTable);
    }

    /**
     * Writes the content of the TableView into a CSV format.
     *
     * @param writer the Writer to output CSV text
     * @param table  the TableView providing columns and items
     * @param title  the title (not written) but may be used in CSV header logic
     * @throws IOException if writing to the writer fails
     */
    protected void writeCsvContent(Writer writer, TableView<T> table, String title) throws IOException {
        List<TableColumn<T, ?>> columns = table.getColumns();
        // Header row
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0)
                writer.write(",");
            writer.write(escapeCsv(columns.get(i).getText()));
        }
        writer.write("\n");
        // Data rows
        for (T item : table.getItems()) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0)
                    writer.write(",");
                writer.write(escapeCsv(getFormattedCellValue(item, columns.get(i))));
            }
            writer.write("\n");
        }
    }

    /**
     * Retrieves and formats the cell value for a given item and column,
     * applying cell factories if provided.
     *
     * @param item   the row data item
     * @param column the TableColumn to extract value from
     * @return the formatted cell text
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected String getFormattedCellValue(Object item, TableColumn<?, ?> column) {
        try {
            if (column.getCellValueFactory() != null) {
                ObservableValue observable = column.getCellValueFactory()
                        .call(new TableColumn.CellDataFeatures(null, column, item));
                if (observable != null) {
                    Object value = observable.getValue();
                    if (value != null && column.getCellFactory() != null) {
                        Callback<TableColumn, TableCell> cellFactory = (Callback<TableColumn, TableCell>) (Object) column
                                .getCellFactory();
                        TableCell cell = cellFactory.call(column);
                        cell.setItem(value);
                        String text = cell.getText();
                        if (text != null && !text.isEmpty()) {
                            return text;
                        }
                    }
                    return value.toString();
                }
            }
            return item != null ? item.toString() : "";
        } catch (Exception e) {
            System.err.println("Error formatting cell value: " + e.getMessage());
            return "";
        }
    }

    /**
     * Escapes special characters in CSV values, enclosing in quotes if necessary.
     *
     * @param value the raw cell text
     * @return the escaped CSV-compliant text
     */
    protected String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
