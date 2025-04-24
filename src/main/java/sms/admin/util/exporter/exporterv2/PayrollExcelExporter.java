/**
 * Abstract base class for exporting payroll data to Excel spreadsheets.
 * <p>
 * Implements the {@link sms.admin.util.exporter.TableDataProvider} interface
 * for {@link dev.finalproject.models.Student} and provides helper methods
 * to create common cell styles (headers, centered text, currency, borders)
 * and to apply borders to a given range within a sheet.
 * </p>
 */
package sms.admin.util.exporter.exporterv2;

import org.apache.poi.ss.usermodel.*;
import dev.finalproject.models.Student;
import sms.admin.util.exporter.TableDataProvider;
import java.util.ArrayList;
import java.util.List;

public abstract class PayrollExcelExporter implements TableDataProvider<Student> {

    /** Peso currency symbol used in formatting. */
    protected static final String PESO = "₱";

    /**
     * Returns the list of header names for the Excel sheet.
     * <p>
     * Subclasses should override to provide actual header values.
     * </p>
     *
     * @return a {@link List} of column header strings
     */
    @Override
    public List<String> getHeaders() {
        return new ArrayList<>();
    }

    /**
     * Returns the row data for a given student.
     * <p>
     * Subclasses should override to map {@link Student} properties to cell values.
     * </p>
     *
     * @param student the {@link Student} whose data will populate the row
     * @return a {@link List} of cell values as strings
     */
    @Override
    public List<String> getRowData(Student student) {
        return new ArrayList<>();
    }

    /**
     * Creates a header cell style with bold, centered text.
     *
     * @param workbook the {@link Workbook} to create the style in
     * @return a {@link CellStyle} configured for headers
     */
    public CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * Creates a cell style with centered horizontal and vertical alignment.
     *
     * @param workbook the {@link Workbook} to create the style in
     * @return a centered {@link CellStyle}
     */
    public CellStyle createCenterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Creates a currency cell style using the Peso symbol and two decimal places.
     *
     * @param workbook the {@link Workbook} to create the style in
     * @return a {@link CellStyle} configured for currency formatting
     */
    public CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat(PESO + "#,##0.00"));
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Creates a cell style with thin borders on all sides and centered text.
     *
     * @param workbook the {@link Workbook} to create the style in
     * @return a bordered {@link CellStyle}
     */
    public CellStyle createBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Applies thin borders to each cell in the specified rectangular range.
     * <p>
     * Existing styles are preserved and augmented with border settings.
     * </p>
     *
     * @param sheet    the {@link Sheet} containing the cells
     * @param startRow zero-based index of the first row
     * @param endRow   zero-based index of the last row
     * @param startCol zero-based index of the first column
     * @param endCol   zero-based index of the last column
     */
    public void applyBorders(Sheet sheet, int startRow, int endRow, int startCol, int endCol) {
        Workbook workbook = sheet.getWorkbook();
        for (int r = startRow; r <= endRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null)
                row = sheet.createRow(r);
            for (int c = startCol; c <= endCol; c++) {
                Cell cell = row.getCell(c);
                if (cell == null)
                    cell = row.createCell(c);

                CellStyle existing = cell.getCellStyle();
                CellStyle style = (existing != null && existing.getIndex() != 0)
                        ? existing
                        : workbook.createCellStyle();

                style.setBorderTop(BorderStyle.THIN);
                style.setBorderBottom(BorderStyle.THIN);
                style.setBorderLeft(BorderStyle.THIN);
                style.setBorderRight(BorderStyle.THIN);

                cell.setCellStyle(style);
            }
        }
    }
}
