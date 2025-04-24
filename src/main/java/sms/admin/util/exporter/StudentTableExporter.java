package sms.admin.util.exporter;

import dev.finalproject.models.Student;
import javafx.collections.ObservableList;
import org.apache.poi.ss.usermodel.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class StudentTableExporter extends BaseTableExporter<Student> {

    @Override
    public String getSheetName() {
        return "Students";
    }

    @Override
    public List<String> getHeaders() {
        return Arrays.asList("ID", "First Name", "Last Name", "Middle Name", "Extension", "Cluster", "Contact",
                "Email");
    }

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

    @Override
    public void writeDataToWorkbook(Workbook workbook, ObservableList<Student> items, String title) {
        Sheet sheet = workbook.createSheet(getSheetName());
        writeBasicSheet(workbook, sheet, items, title);
    }

    @Override
    public void writeDataToPdf(Document document, ObservableList<Student> items, String title) {
        writeBasicPdf(document, items, title);
    }

    @Override
    public void writeDataToCsv(PrintWriter writer, ObservableList<Student> items, String title) {
        writeBasicCsv(writer, items, title);
    }

    protected void writeBasicSheet(Workbook workbook, Sheet sheet, ObservableList<Student> items, String title) {
        int rowNum = 0;

        // Write title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);

        // Write headers
        Row headerRow = sheet.createRow(rowNum++);
        List<String> headers = getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
        }

        // Write data
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

    protected void writeBasicPdf(Document document, ObservableList<Student> items, String title) {
        document.add(new Paragraph(title));

        List<String> headers = getHeaders();
        Table table = new Table(headers.size());

        // Add headers
        for (String header : headers) {
            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)));
        }

        // Add data
        for (Student item : items) {
            List<String> rowData = getRowData(item);
            for (String value : rowData) {
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(value)));
            }
        }

        document.add(table);
    }

    protected void writeBasicCsv(PrintWriter writer, ObservableList<Student> items, String title) {
        writer.println(title);
        writer.println();

        // Write headers
        List<String> headers = getHeaders();
        writer.println(String.join(",", headers));

        // Write data
        for (Student item : items) {
            List<String> rowData = getRowData(item);
            writer.println(String.join(",", rowData));
        }
    }
}
