package sms.admin.util.exporter;

import dev.finalproject.models.Student;
import javafx.scene.control.TableColumn;
import org.apache.poi.ss.usermodel.*;

public class PayrollTableExporter extends BaseTableExporter<Student> {
    
    @Override
    protected String getSheetName() {
        return "Payroll";
    }
    
    @Override
    protected void formatExcelCell(Workbook workbook, Cell cell, String value, TableColumn<Student, ?> column) {
        if (column.getText().contains("Fare") || column.getText().contains("Amount")) {
            try {
                double numValue = Double.parseDouble(value.replaceAll("[^\\d.]", ""));
                cell.setCellValue(numValue);
                
                CellStyle currencyStyle = workbook.createCellStyle();
                DataFormat format = workbook.createDataFormat();
                currencyStyle.setDataFormat(format.getFormat("₱#,##0.00"));
                cell.setCellStyle(currencyStyle);
            } catch (NumberFormatException e) {
                cell.setCellValue(value);
            }
        } else {
            cell.setCellValue(value);
        }
    }
}
