package sms.admin.util.exporter;

import java.io.IOException;
import javafx.scene.control.TableView;

public abstract class BaseTableExporter<T> implements TableDataProvider<T> {

    private final UnifiedTableExporter<T> exporter = new UnifiedTableExporter<>();

    public void exportToExcel(TableView<T> table, String title, String outputPath) {
        try {
            exporter.exportToExcel(table, title, outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to export to Excel: " + e.getMessage());
        }
    }

    public void exportToPdf(TableView<T> table, String title, String outputPath) {
        try {
            exporter.exportToPdf(table, title, outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to export to PDF: " + e.getMessage());
        }
    }

    public void exportToCsv(TableView<T> table, String title, String outputPath) {
        try {
            exporter.exportToCsv(table, title, outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to export to CSV: " + e.getMessage());
        }
    }
}
