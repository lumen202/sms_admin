package sms.admin.util.exporter.exporterv2;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import dev.finalproject.models.Student;
import javafx.collections.ObservableList;

public abstract class PayrollPdfExporter {
    
    protected static final String PESO = "₱";

    protected abstract void writeHeaderSection(Document document);
    protected abstract void writeTableContent(Document document, Table table, ObservableList<Student> items);
    protected abstract void writeFooterSection(Document document);

    protected Table createBaseTable(int numColumns) {
        Table table = new Table(UnitValue.createPercentArray(numColumns));
        table.setWidth(UnitValue.createPercentValue(100));
        return table;
    }

    protected Cell createHeaderCell(String text, int rowspan, int colspan) {
        return new Cell(rowspan, colspan)
            .add(new Paragraph(text))
            .setTextAlignment(TextAlignment.CENTER)
            .setBold();  // Add bold style here in base class
    }

    protected Cell createDataCell(String text, TextAlignment alignment) {
        return new Cell()
            .add(new Paragraph(text))
            .setTextAlignment(alignment);
    }

    public void exportToPdf(Document document, ObservableList<Student> items, String title) {
        writeHeaderSection(document);
        Table table = createBaseTable(37); // Adjust columns as needed
        writeTableContent(document, table, items);
        document.add(table);
        writeFooterSection(document);
    }
}
