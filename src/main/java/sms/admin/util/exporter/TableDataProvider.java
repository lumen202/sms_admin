/**
 * Defines a contract for providing tabular data and exporting it
 * to various formats such as Excel, PDF, and CSV.
 * <p>
 * Implementations supply the table's headers, row data, and handle
 * writing the data into different export formats.
 * </p>
 *
 * @param <T> the type of the data item for each table row
 */
package sms.admin.util.exporter;

import java.util.List;
import org.apache.poi.ss.usermodel.Workbook;
import com.itextpdf.layout.Document;
import java.io.PrintWriter;
import javafx.collections.ObservableList;

public interface TableDataProvider<T> {

    /**
     * Returns the name of the sheet or section when exporting to Excel.
     *
     * @return the sheet name for the workbook
     */
    String getSheetName();

    /**
     * Returns the list of column headers for the table.
     *
     * @return a List of header labels
     */
    List<String> getHeaders();

    /**
     * Maps a data item to a list of string values for each table cell in the row.
     *
     * @param item the data object representing one row
     * @return a List of cell values as strings
     */
    List<String> getRowData(T item);

    /**
     * Writes the provided items into an Excel {@link Workbook}, using the given
     * title.
     *
     * @param workbook the Apache POI Workbook instance
     * @param items    the collection of data items to export
     * @param title    a title or header to include at the top of the sheet
     */
    void writeDataToWorkbook(Workbook workbook, ObservableList<T> items, String title);

    /**
     * Writes the provided items into a PDF {@link Document}, using the given title.
     *
     * @param document the iText PDF Document instance
     * @param items    the collection of data items to export
     * @param title    a title or header to include at the top of the PDF
     */
    void writeDataToPdf(Document document, ObservableList<T> items, String title);

    /**
     * Writes the provided items into a CSV format via {@link PrintWriter}, using
     * the given title.
     *
     * @param writer the PrintWriter to write CSV content
     * @param items  the collection of data items to export
     * @param title  a title or header to include at the top of the CSV output
     */
    void writeDataToCsv(PrintWriter writer, ObservableList<T> items, String title);
}
