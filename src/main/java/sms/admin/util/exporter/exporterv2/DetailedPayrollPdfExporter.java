package sms.admin.util.exporter.exporterv2;

import com.itextpdf.io.exceptions.IOException;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.kernel.colors.ColorConstants;
import sms.admin.util.attendance.AttendanceUtil;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.time.YearMonth;
import java.io.InputStream;

public class DetailedPayrollPdfExporter extends PayrollPdfExporter {

    private static final String CHECKMARK = "\u2713";
    private final YearMonth period;
    private final ObservableList<AttendanceLog> attendanceLogs;

    public DetailedPayrollPdfExporter(YearMonth period, ObservableList<AttendanceLog> attendanceLogs) {
        this.period = period;
        this.attendanceLogs = attendanceLogs;
    }

    @Override
    protected void writeHeaderSection(Document document) {
        try {
            // Load the font
            InputStream fontStream = getClass().getResourceAsStream("/sms/admin/assets/fonts/arialuni.ttf");
            if (fontStream == null) {
                throw new IOException("Font file not found in resources.");
            }
            byte[] fontBytes = fontStream.readAllBytes();
            PdfFont font = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

            document.setFont(font);

            document.add(new Paragraph("GENERAL FORM NO. 7(A)")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12));
            document.add(new Paragraph("TIME BOOK AND PAYROLL")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14)
                    .setBold());
            document.add(new Paragraph(
                    "For labor on Baybay Data Center at Baybay Nat'l High School, Baybay City, Leyte, Philippines, for the period")
                    .add(new Paragraph(period.getMonth().toString().toUpperCase() + " " + period.getYear())
                            .setFontSize(12)
                            .setBold())
                    .setTextAlignment(TextAlignment.CENTER));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void writeTableContent(Document document, Table table, ObservableList<Student> items) {
        // Create table with proper column widths
        float[] columnWidths = new float[] {
                20f, // No.
                80f, // Name
                20f, 20f, 20f, 20f, 20f, // Week days
                30f, // Total Days
                40f, // Transportation
                40f, // Meal Allowance
                40f, // Total Amount
                20f, // No.
                30f // Signature
        };
        Table detailedTable = new Table(UnitValue.createPercentArray(columnWidths));
        detailedTable.setWidth(UnitValue.createPercentValue(100));
        detailedTable.useAllAvailableWidth();

        // Add headers
        addTableHeaders(detailedTable);

        // Add data rows
        double grandTotal = 0;
        for (Student student : items) {
            grandTotal += addDataRow(detailedTable, student);
        }

        // Add subtotal row
        detailedTable.addCell(
                new Cell(1, columnWidths.length - 2)
                        .add(new Paragraph("SUB-TOTAL FOR THIS PAGE"))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setBorder(null));
        detailedTable.addCell(
                new Cell(1, 2)
                        .add(new Paragraph(PESO + String.format("%,.2f", grandTotal)))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBorder(null));

        // Add table to document
        document.add(detailedTable);
    }

    @Override
    protected void writeFooterSection(Document document) {
        document.add(new Paragraph(
                "1. I HEREBY CERTIFY on my official oath to the correctness of the above roll. Payment is hereby approved from the appropriation indicated.")
                .setTextAlignment(TextAlignment.LEFT));
        document.add(new Paragraph("2. APPROVED")
                .setTextAlignment(TextAlignment.LEFT));
        document.add(new Paragraph(
                "3. I HEREBY CERTIFY on my official oath that I have this ___ day of ____ paid in cash to each man whose name appears on the above roll, the amount set opposite his name, he having presented himself, established his identity and affixed his signature or thumbmark on the space provided therefor.")
                .setTextAlignment(TextAlignment.LEFT));
        document.add(new Paragraph("JESCYN KATE N. RAMOS, E2P - ICT Coordinator")
                .setTextAlignment(TextAlignment.LEFT));
        document.add(new Paragraph("CARLOS JERICHO L. PETILLA, Provincial Governor")
                .setTextAlignment(TextAlignment.LEFT));
        document.add(new Paragraph("JOAN FLORLYN JUSTISA, E2P - ICT")
                .setTextAlignment(TextAlignment.LEFT));
        document.add(new Paragraph(
                "NOTE: Where thumbmark is to be used in place of signature, and the space available is not sufficient, the thumbmark may be impressed on the back hereof with proper indication of the corresponding student's name and on the corresponding line on the payroll or remark 'see thumbmark on the back' should be written.")
                .setTextAlignment(TextAlignment.LEFT));
        document.add(new Paragraph("'IPAKITA SA MUNDO, UMAASENSA NA TAYO'")
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void addTableHeaders(Table table) {
        // Use super.createHeaderCell instead of local method
        table.addHeaderCell(super.createHeaderCell("No.", 3, 1));
        table.addHeaderCell(super.createHeaderCell("Name", 3, 1));

        // Week columns
        for (int i = 1; i <= 5; i++) {
            table.addHeaderCell(super.createHeaderCell("Week " + i, 1, 1));
        }

        table.addHeaderCell(super.createHeaderCell("Total Days", 3, 1));
        table.addHeaderCell(super.createHeaderCell("Transportation", 1, 2));
        table.addHeaderCell(super.createHeaderCell("Meal", 1, 2));
        table.addHeaderCell(super.createHeaderCell("Total Amount", 3, 1));
        table.addHeaderCell(super.createHeaderCell("No.", 3, 1));
        table.addHeaderCell(super.createHeaderCell("Signature", 3, 1));

        // Sub-headers for allowances
        table.addHeaderCell(super.createHeaderCell("Rate", 1, 1));
        table.addHeaderCell(super.createHeaderCell("Amount", 1, 1));
        table.addHeaderCell(super.createHeaderCell("Rate", 1, 1));
        table.addHeaderCell(super.createHeaderCell("Amount", 1, 1));
    }

    private double addDataRow(Table table, Student student) {
        double totalDays = 0;

        // Serial Number and Name
        table.addCell(new Cell()
                .add(new Paragraph(String.valueOf(student.getStudentID())))
                .setTextAlignment(TextAlignment.CENTER));

        String fullName = student.getLastName() + ", " + student.getFirstName() +
                (student.getMiddleName() != null ? " " + student.getMiddleName() : "") +
                (student.getNameExtension() != null ? " " + student.getNameExtension() : "");
        table.addCell(new Cell()
                .add(new Paragraph(fullName))
                .setTextAlignment(TextAlignment.LEFT));

        // Add cell borders and proper spacing
        Cell dataCell = new Cell()
                .setBorder(getBorder())
                .setPadding(5)
                .setMargin(2);

        // ... continue with existing code but use the dataCell template ...
        return totalDays;
    }

    // Add helper method for cell borders
    private Border getBorder() {
        return new SolidBorder(ColorConstants.BLACK, 0.5f);
    }
}
