package sms.admin.util.exporter;

import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.beans.value.ObservableValue;
import javafx.util.Callback;

public abstract class AbstractTableExporter {
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected String getFormattedCellValue(Object item, TableColumn<?, ?> column) {
        try {
            // Get value using cell value factory
            if (column.getCellValueFactory() != null) {
                ObservableValue observable = column.getCellValueFactory()
                    .call(new TableColumn.CellDataFeatures(null, column, item));
                    
                if (observable != null) {
                    Object value = observable.getValue();
                    
                    // Format using cell factory if available
                    if (value != null && column.getCellFactory() != null) {
                        // Get cell factory with proper type casting
                        Callback<TableColumn, TableCell> cellFactory = 
                            (Callback<TableColumn, TableCell>)(Object)column.getCellFactory();
                            
                        // Create and setup cell
                        TableCell cell = cellFactory.call(column);
                        if (cell != null) {
                            cell.setItem(value);
                            String text = cell.getText();
                            if (text != null && !text.isEmpty()) {
                                return text;
                            }
                        }
                        return value.toString();
                    }
                    return value != null ? value.toString() : "";
                }
            }
            
            return item != null ? item.toString() : "";
            
        } catch (Exception e) {
            System.err.println("Error formatting cell value: " + e.getMessage());
            return "";
        }
    }

    public abstract void exportToExcel(TableView<?> table, String title, String outputPath);
    public abstract void exportToPdf(TableView<?> table, String title, String outputPath);
    public abstract void exportToCsv(TableView<?> table, String title, String outputPath);
}
