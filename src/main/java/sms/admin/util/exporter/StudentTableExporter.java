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
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

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
                "ID", "First Name", "Last Name", "Middle Name",
                "Extension", "Cluster", "Contact", "Email");
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
        return Arrays.asList(
                String.valueOf(student.getStudentID()),
                student.getFirstName(),
                student.getLastName(),
                student.getMiddleName(),
                student.getNameExtension(),
                student.getClusterID().getClusterName(),
                student.getContact(),
                student.getEmail());
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
        Sheet sheet = workbook.createSheet(getSheetName());
        writeBasicSheet(workbook, sheet, items, title);
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
     * Writes the core PDF content: title and a table with headers and row data.
     *
     * @param document the PDF Document to populate
     * @param items    the data rows to write
     * @param title    the document title
     */
    protected void writeBasicPdf(Document document, ObservableList<Student> items, String title) {
        document.add(new Paragraph(title));

        List<String> headers = getHeaders();
        Table table = new Table(headers.size());

        // Add header cells
        for (String header : headers) {
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)));
        }

        // Add data cells
        for (Student item : items) {
            List<String> rowData = getRowData(item);
            for (String value : rowData) {
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(value)));
            }
        }

        document.add(table);
    }

    /**
     * Writes the core CSV content: title, blank line, headers, and row data.
     *
     * @param writer the PrintWriter to write CSV data to
     * @param items  the data rows to write
     * @param title  the CSV title
     */
    protected void writeBasicCsv(PrintWriter writer, ObservableList<Student> items, String title) {
        writer.println(title);
        writer.println();

        List<String> headers = getHeaders();
        writer.println(String.join(",", headers));

        for (Student item : items) {
            List<String> rowData = getRowData(item);
            writer.println(String.join(",", rowData));
        }
    }
}
