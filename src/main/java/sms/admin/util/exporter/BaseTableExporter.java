package sms.admin.util.exporter;

import javafx.scene.control.TableView;

public abstract class BaseTableExporter<T> implements TableDataProvider<T> {

    public void exportToExcel(TableView<T> table, String title, String outputPath) {
        // Implementation will be added later if needed
    }

    public void exportToPdf(TableView<T> table, String title, String outputPath) {
        // Implementation will be added later if needed
    }

    public void exportToCsv(TableView<T> table, String title, String outputPath) {
        // Implementation will be added later if needed
    }
}
