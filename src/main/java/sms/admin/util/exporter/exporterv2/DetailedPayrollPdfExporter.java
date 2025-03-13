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
import java.util.List;
import java.util.ArrayList;
import java.time.temporal.WeekFields;

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

            // Form Number
            Paragraph formNumber = new Paragraph("GENERAL FORM NO. 7(A)")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(14f)  // Increased from 12
                .setMargin(0)
                .setMultipliedLeading(1.0f);
            document.add(formNumber);

            // Title
            Paragraph title = new Paragraph("TIME BOOK AND PAYROLL")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18f)  // Increased from 14
                .setBold()
                .setMargin(0)
                .setMultipliedLeading(1.2f);  // Changed to float
            document.add(title);

            // Location and Period text
            float textWidth = document.getPdfDocument().getDefaultPageSize().getWidth() - 100; // Margins
            float baseFontSize = 12f;
            float locationFontSize = Math.min(baseFontSize, (textWidth / 50)); // Adjust divisor as needed

            // Location and Period
            Paragraph location = new Paragraph()
                .add("For labor on Baybay Data Center at Baybay Nat'l High School, Baybay City, Leyte, Philippines, for the period ")
                .add(new Paragraph(period.getMonth().toString().toUpperCase() + " " + period.getYear()).setBold())
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(locationFontSize)
                .setMarginTop(10)
                .setMarginBottom(20);
            document.add(location);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void writeTableContent(Document document, Table table, ObservableList<Student> items) {
        // Adjusted column widths with more space for name
        float[] columnWidths = new float[] {
                15f,  // No.
                120f, // Name (increased)
                15f, 15f, 15f, 15f, 15f, // Week days
                25f,  // Total Days
                35f,  // Transportation
                35f,  // Meal Allowance
                35f,  // Total Amount
                15f,  // No.
                25f   // Signature
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
        List<List<LocalDate>> weeks = getWorkWeeks();
        int totalWorkDays = weeks.stream().mapToInt(List::size).sum();

        // Main column headers
        table.addHeaderCell(super.createHeaderCell("No.", 4, 1));
        table.addHeaderCell(super.createHeaderCell("Name", 4, 1));
        table.addHeaderCell(super.createHeaderCell("TIME ROLL", 1, totalWorkDays));

        // Add weeks and their days
        for (int i = 0; i < weeks.size(); i++) {
            List<LocalDate> week = weeks.get(i);
            if (!week.isEmpty()) {
                // Week header
                table.addHeaderCell(super.createHeaderCell("Week " + (i + 1), 1, week.size()));
                
                // Create array to hold day cells for this week
                Cell[] dayCells = new Cell[week.size()];
                Cell[] dateCells = new Cell[week.size()];
                
                // First pass: Create day name cells
                for (int j = 0; j < week.size(); j++) {
                    LocalDate date = week.get(j);
                    dayCells[j] = super.createHeaderCell(
                        date.getDayOfWeek().toString().substring(0, 2), 1, 1);
                }
                
                // Second pass: Create date number cells
                for (int j = 0; j < week.size(); j++) {
                    LocalDate date = week.get(j);
                    dateCells[j] = super.createHeaderCell(
                        String.valueOf(date.getDayOfMonth()), 1, 1);
                }
                
                // Add all day cells first, then all date cells
                for (Cell dayCell : dayCells) {
                    table.addHeaderCell(dayCell);
                }
                for (Cell dateCell : dateCells) {
                    table.addHeaderCell(dateCell);
                }
            }
        }

        // Single column for Total Days
        table.addHeaderCell(super.createHeaderCell("Total No. of Days", 4, 1));

        // Transportation headers
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

    private List<List<LocalDate>> getWorkWeeks() {
        List<List<LocalDate>> weeks = new ArrayList<>();
        List<LocalDate> currentWeek = new ArrayList<>();
        LocalDate firstDay = period.atDay(1);
        
        // Initialize first week
        int currentWeekNum = firstDay.get(WeekFields.ISO.weekOfWeekBasedYear());
        
        for (int day = 1; day <= period.lengthOfMonth(); day++) {
            LocalDate date = period.atDay(day);
            
            // Skip weekends
            if (AttendanceUtil.isWeekend(date)) {
                continue;
            }
            
            int weekNum = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            
            // If new week starts, save current week and start new one
            if (weekNum != currentWeekNum && !currentWeek.isEmpty()) {
                weeks.add(new ArrayList<>(currentWeek));
                currentWeek.clear();
                currentWeekNum = weekNum;
            }
            
            currentWeek.add(date);
        }
        
        // Add last week if not empty
        if (!currentWeek.isEmpty()) {
            weeks.add(currentWeek);
        }
        
        return weeks;
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
