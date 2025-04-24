package sms.admin.util.exporter;

import java.util.List;
import org.apache.poi.ss.usermodel.Workbook;
import com.itextpdf.layout.Document;
import java.io.PrintWriter;
import javafx.collections.ObservableList;

public interface TableDataProvider<T> {
    String getSheetName();
    List<String> getHeaders();
    List<String> getRowData(T item);
    
    // Add these abstract methods
    void writeDataToWorkbook(Workbook workbook, ObservableList<T> items, String title);
    void writeDataToPdf(Document document, ObservableList<T> items, String title);
    void writeDataToCsv(PrintWriter writer, ObservableList<T> items, String title);
}
