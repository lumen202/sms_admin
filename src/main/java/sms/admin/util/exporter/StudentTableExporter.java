package sms.admin.util.exporter;

import dev.finalproject.models.Student;
import javafx.collections.ObservableList;
import org.apache.poi.ss.usermodel.*;
import com.itextpdf.layout.Document;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class StudentTableExporter extends BaseTableExporter<Student> implements TableDataProvider<Student> {

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
}
