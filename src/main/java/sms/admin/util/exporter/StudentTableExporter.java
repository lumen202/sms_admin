/**
 * Concrete exporter for Student data to multiple formats: Excel, PDF, and CSV.
 * <p>
 * Extends {@link BaseTableExporter} to provide specific implementations for
 * exporting {@link dev.finalproject.models.Student} records. Supports writing
 * to Apache POI Workbooks, iText PDF Documents, and CSV via PrintWriter.
 * </p>
 */
package sms.admin.util.exporter;

import dev.finalproject.models.Student;
import javafx.collections.ObservableList;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.font.constants.StandardFonts;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StudentTableExporter extends BaseTableExporter<Student> {

    /**
     * {@inheritDoc}
     * 
     * @return the name of the Excel sheet for student data
     */
    @Override
    public String getSheetName() {
        return "Students";
    }

    /**
     * {@inheritDoc}
     * 
     * @return list of column header labels for student export
     */
    @Override
    public List<String> getHeaders() {
        return Arrays.asList(
                "Timestamp", "Email", "FirstName", "MiddleName", "LastName",
                "Address", "Cluster", "Contact");
    }

    /**
     * {@inheritDoc}
     * Maps a {@link Student} object's fields to a list of cell values.
     *
     * @param student the Student to export
     * @return list of string representations of student fields
     */
    @Override
    public List<String> getRowData(Student student) {
        // Get current timestamp in desired format
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss"));

        return Arrays.asList(
                timestamp,
                student.getEmail(),
                student.getFirstName(),
                student.getMiddleName(),
                student.getLastName(),
                student.getClusterID().getClusterName(),
                student.getContact());
    }

    /**
     * {@inheritDoc}
     * Creates an Excel sheet and writes student data with headers and autosized
     * columns.
     *
     * @param workbook the POI Workbook to write into
     * @param items    the list of Student objects to export
     * @param title    the title to place at the top of the sheet
     */
    @Override
    public void writeDataToWorkbook(Workbook workbook, ObservableList<Student> items, String title) {
        throw new UnsupportedOperationException("Excel export is not supported");
    }

    /**
     * {@inheritDoc}
     * Writes student data into a PDF {@link Document} with a title paragraph and
     * table.
     *
     * @param document the iText PDF Document to add content to
     * @param items    the list of Student objects to export
     * @param title    the title to add above the table
     */
    @Override
    public void writeDataToPdf(Document document, ObservableList<Student> items, String title) {
        writeBasicPdf(document, items, title);
    }

    /**
     * {@inheritDoc}
     * Writes student data to a CSV format via {@link PrintWriter}.
     *
     * @param writer the PrintWriter to write CSV lines to
     * @param items  the list of Student objects to export
     * @param title  the title to include at the top of the CSV
     */
    @Override
    public void writeDataToCsv(PrintWriter writer, ObservableList<Student> items, String title) {
        writeBasicCsv(writer, items, title);
    }

    /**
     * Writes the core Excel sheet content: title, headers, rows, and column sizing.
     *
     * @param workbook the POI Workbook containing the sheet
     * @param sheet    the Sheet to populate
     * @param items    the data rows to write
     * @param title    the sheet title
     */
    protected void writeBasicSheet(Workbook workbook, Sheet sheet,
            ObservableList<Student> items, String title) {
        int rowNum = 0;

        // Write title row
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);

        // Write header row
        Row headerRow = sheet.createRow(rowNum++);
        List<String> headers = getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
        }

        // Write data rows
        for (Student item : items) {
            Row row = sheet.createRow(rowNum++);
            List<String> rowData = getRowData(item);
            for (int i = 0; i < rowData.size(); i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(rowData.get(i));
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Writes the core CSV content: title, blank line, headers, and row data.
     *
     * @param writer the PrintWriter to write CSV data to
     * @param items  the data rows to write
     * @param title  the CSV title
     */
    protected void writeBasicCsv(PrintWriter writer, ObservableList<Student> items, String title) {
        // Skip title and blank line for this format
        List<String> headers = getHeaders();
        writer.println(String.join(",", headers));

        for (Student item : items) {
            List<String> rowData = getRowData(item);
            // Properly escape and quote values
            String row = rowData.stream()
                    .map(value -> {
                        if (value == null)
                            value = "";
                        // Quote values containing commas or quotes
                        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                            return "\"" + value.replace("\"", "\"\"") + "\"";
                        }
                        return value;
                    })
                    .collect(Collectors.joining(","));
            writer.println(row);
        }
    }

    /**
     * Writes the core PDF content: title, headers, rows, and summary footer.
     *
     * @param document the iText PDF Document to add content to
     * @param items    the data rows to write
     * @param title    the PDF title
     */
    protected void writeBasicPdf(Document document, ObservableList<Student> items, String title) {
        try {
            // Set document margins (further reduced)
            document.setMargins(25, 25, 25, 25); // Reduced from 30 to 25

            // Set up fonts
            PdfFont headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Add title with smaller font
            Paragraph titlePara = new Paragraph(title)
                    .setFont(headerFont)
                    .setFontSize(12) // Reduced from 14
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(8); // Reduced from 10
            document.add(titlePara);

            // Add timestamp with smaller font
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a"));
            Paragraph datePara = new Paragraph("Generated on: " + timestamp)
                    .setFont(normalFont)
                    .setFontSize(7) // Reduced from 8
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(8); // Reduced from 10
            document.add(datePara);

            // Further optimized column widths
            float[] columnWidths = new float[] {
                    6, // ID (reduced)
                    12, // First Name
                    12, // Last Name
                    12, // Middle Name
                    6, // Extension (reduced)
                    24, // Cluster (increased for better readability)
                    14, // Contact
                    14 // Email
            };

            Table table = new Table(UnitValue.createPercentArray(columnWidths))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(8)
                    .setFontSize(7) // Reduced from 8
                    .setAutoLayout();

            // Style headers
            DeviceRgb headerBgColor = new DeviceRgb(0, 51, 102);
            List<String> headers = getHeaders();
            for (String header : headers) {
                table.addHeaderCell(
                        new com.itextpdf.layout.element.Cell()
                                .setBackgroundColor(headerBgColor)
                                .setFontColor(ColorConstants.WHITE)
                                .setFont(headerFont)
                                .setFontSize(7) // Reduced from 8
                                .setPadding(2) // Reduced from 3
                                .add(new Paragraph(header))
                                .setKeepTogether(true));
            }

            // Add data with smaller font and padding
            DeviceRgb altRowColor = new DeviceRgb(240, 240, 240);
            boolean alternate = false;

            for (Student item : items) {
                List<String> rowData = getRowData(item);
                for (String value : rowData) {
                    com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                            .setFont(normalFont)
                            .setFontSize(7) // Reduced from 8
                            .setPadding(2) // Reduced from 3
                            .setKeepTogether(true);

                    if (alternate) {
                        cell.setBackgroundColor(altRowColor);
                    }

                    Paragraph cellContent = new Paragraph(value)
                            .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.LEFT);

                    cell.add(cellContent);
                    table.addCell(cell);
                }
                alternate = !alternate;
            }

            // Enable table splitting across pages
            table.setKeepTogether(false);

            // Add summary footer
            Paragraph summary = new Paragraph(String.format("Total Students: %d", items.size()))
                    .setFont(headerFont)
                    .setFontSize(8) // Reduced from 9
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(6); // Reduced from 8

            document.add(table);
            document.add(summary);

        } catch (Exception e) {
            throw new RuntimeException("Error creating PDF: " + e.getMessage(), e);
        }
    }
}
