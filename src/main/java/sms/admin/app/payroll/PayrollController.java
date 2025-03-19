package sms.admin.app.payroll;

import java.text.DecimalFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import com.itextpdf.layout.Document;

import dev.finalproject.App;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.Node;
import sms.admin.app.RootController;
import sms.admin.app.attendance.AttendanceController;
import sms.admin.util.YearData;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.exporter.PayrollTableExporter;
import sms.admin.util.mock.DataUtil;
import sms.admin.util.exporter.exporterv2.DetailedPayrollExporter;
import sms.admin.util.exporter.exporterv2.DetailedPayrollPdfExporter;
import java.io.FileOutputStream;

// Add these imports at the top
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
// ...existing imports...

public class PayrollController extends FXController {

    @FXML
    private ComboBox<String> yearMonthComboBox;
    @FXML
    private AnchorPane rootPane;
    @FXML
    private TableView<Student> payrollTable;
    @FXML
    private TableColumn<Student, Integer> colNo;
    @FXML
    private TableColumn<Student, String> colFullName;
    @FXML
    private TableColumn<Student, Integer> colTotalDays;
    @FXML
    private TableColumn<Student, Double> colFare;
    @FXML
    private TableColumn<Student, Double> colTotalAmount;
    @FXML
    private Label totalAmountLabel;
    @FXML
    private MenuButton exportButton;
    @FXML
    private MenuItem exportExcel;
    @FXML
    private MenuItem exportCsv;
    @FXML
    private MenuItem exportPdf;
    @FXML
    private MenuItem exportDetailedExcel; // Add this field
    @FXML
    private MenuItem exportDetailedPdf; // Add this field

    private ObservableList<Student> masterStudentList;
    private FilteredList<Student> filteredStudentList;
    private ObservableList<AttendanceLog> attendanceLog;

    private final DecimalFormat daysFormat = new DecimalFormat("#.# day(s)");
    private final DecimalFormat currencyFormat = new DecimalFormat("₱#,##0.00");

    @Override
    protected void load_fields() {
        rootPane.getProperties().put("controller", this);
        
        // Get filtered list from parameters
        filteredStudentList = (FilteredList<Student>) getParameter("filteredStudentList");
        attendanceLog = (ObservableList<AttendanceLog>) getParameter("attendanceLogList");

        setupTable();

        String selectedYear = (String) getParameter("selectedYear");
        if (selectedYear == null) {
            selectedYear = DateTimeUtils.getCurrentAcademicYear();
        }

        String selectedMonth = (String) getParameter("selectedMonth");
        if (selectedMonth != null && yearMonthComboBox.getItems().contains(selectedMonth)) {
            yearMonthComboBox.setValue(selectedMonth);
            updateRootController(selectedMonth);
        }
    }

    private void updateRootController(String monthYear) {
        Scene scene = rootPane.getScene();
        if (scene != null) {
            Parent root = scene.getRoot();
            if (root != null) {
                Object controller = root.getProperties().get("controller");
                if (controller instanceof RootController rootController) {
                    rootController.setSelectedMonth(monthYear);
                }
            }
        }
    }

    private void setupTable() {
        // Configure columns
        colNo.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        colFullName.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            String fullName = String.format("%s, %s %s",
                    student.getLastName(),
                    student.getFirstName(),
                    student.getMiddleName());
            return new SimpleStringProperty(fullName);
        });

        // Update total days formatting
        colTotalDays.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            double totalDays = calculateTotalDays(student);
            return new SimpleIntegerProperty((int) (totalDays * 10)).asObject(); // Multiply by 10 to preserve decimal
        });
        colTotalDays.setCellFactory(column -> new TableCell<Student, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    double days = item / 10.0; // Convert back to actual value
                    setText(daysFormat.format(days));
                }
            }
        });

        // Update fare formatting
        colFare.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            return new SimpleDoubleProperty(student.getFare()).asObject(); // Add asObject()
        });
        colFare.setCellFactory(column -> new TableCell<Student, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(item));
                }
            }
        });

        // Update total amount formatting
        colTotalAmount.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            double totalDays = calculateTotalDays(student);
            double fare = student.getFare();
            return new SimpleDoubleProperty(totalDays * fare).asObject(); // Add asObject()
        });
        colTotalAmount.setCellFactory(column -> new TableCell<Student, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(item));
                }
            }
        });

        // Set items to filtered list instead of direct student list
        payrollTable.setItems(filteredStudentList);
        updateTotalAmount();
    }

    private void updateTotalAmount() {
        double totalAmount = filteredStudentList.stream()
                .mapToDouble(student -> {
                    double totalDays = calculateTotalDays(student);
                    return totalDays * student.getFare();
                })
                .sum();

        totalAmountLabel.setText(currencyFormat.format(totalAmount));
    }

    private double calculateTotalDays(Student student) {
        try {
            String monthYearValue = yearMonthComboBox.getValue();
            if (monthYearValue == null || monthYearValue.trim().isEmpty()) {
                return 0;
            }

            YearMonth selectedMonth = DateTimeUtils.parseMonthYear(monthYearValue);
            double totalDays = 0;

            // Filter and validate logs first
            List<AttendanceLog> validLogs = attendanceLog.stream()
                .filter(log -> {
                    if (log == null || log.getRecordID() == null || log.getStudentID() == null) {
                        return false;
                    }
                    AttendanceRecord record = log.getRecordID();
                    try {
                        LocalDate.of(record.getYear(), record.getMonth(), record.getDay());
                        return true;
                    } catch (DateTimeException e) {
                        return false;
                    }
                })
                .filter(log -> log.getStudentID().getStudentID() == student.getStudentID())
                .collect(Collectors.toList());

            // Process only valid logs
            for (AttendanceLog log : validLogs) {
                AttendanceRecord record = log.getRecordID();
                YearMonth logMonth = YearMonth.of(record.getYear(), record.getMonth());
                
                if (logMonth.equals(selectedMonth)) {
                    String status = AttendanceUtil.getAttendanceStatus(
                        student, 
                        LocalDate.of(record.getYear(), record.getMonth(), record.getDay()),
                        attendanceLog
                    );
                    
                    totalDays += switch (status) {
                        case AttendanceUtil.PRESENT_MARK, AttendanceUtil.EXCUSED_MARK -> 1.0;
                        case AttendanceUtil.HALF_DAY_MARK -> 0.5;
                        default -> 0.0;
                    };
                }
            }

            return totalDays;
        } catch (Exception e) {
            System.err.println("Error calculating total days for student " + student.getStudentID() + 
                ": " + e.getMessage());
            return 0;
        }
    }

    @Override
    protected void load_bindings() {
        System.out.println("PayrollController.load_bindings called");
        if (rootPane != null) {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    rootPane.prefWidthProperty().bind(newScene.widthProperty());
                    rootPane.prefHeightProperty().bind(newScene.heightProperty());
                }
            });
        }
    }

    @Override
    protected void load_listeners() {
        yearMonthComboBox.setOnAction(event -> {
            if (!yearMonthComboBox.isFocused()) {
                return; // Ignore programmatic changes
            }
            String newValue = yearMonthComboBox.getValue();
            payrollTable.refresh();
            updateTotalAmount();
            updateRootController(newValue);
        });

        // Add export menu item handlers
        exportExcel.setOnAction(event -> handleExport("excel"));
        exportCsv.setOnAction(event -> handleExport("csv"));
        exportPdf.setOnAction(event -> handleExport("pdf"));
        exportDetailedExcel.setOnAction(event -> handleDetailedExport("excel"));
        exportDetailedPdf.setOnAction(event -> handleDetailedExport("pdf")); // Add this line
    }

    private void handleExport(String type) {
        try {
            String selectedMonthYear = yearMonthComboBox.getValue();
            if (selectedMonthYear == null)
                return;

            String title = "Payroll Report - " + selectedMonthYear;
            String fileName = String.format("payroll_%s.%s",
                    selectedMonthYear.replace(" ", "_").toLowerCase(),
                    type.equals("excel") ? "xlsx" : type);
            String outputPath = System.getProperty("user.home") + "/Downloads/" + fileName;

            PayrollTableExporter exporter = new PayrollTableExporter();
            switch (type) {
                case "excel" -> exporter.exportToExcel(payrollTable, title, outputPath);
                case "pdf" -> exporter.exportToPdf(payrollTable, title, outputPath);
                case "csv" -> exporter.exportToCsv(payrollTable, title, outputPath);
            }

            System.out.println("Export completed: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDetailedExport(String type) {
        try {
            String selectedMonthYear = yearMonthComboBox.getValue();
            if (selectedMonthYear == null || selectedMonthYear.trim().isEmpty()) {
                return;
            }

            YearMonth selectedMonth = DateTimeUtils.parseMonthYear(selectedMonthYear);
            String fileName = String.format("detailed_payroll_%s.%s",
                    selectedMonthYear.replace(" ", "_").toLowerCase(),
                    type.equals("excel") ? "xlsx" : "pdf");
            String outputPath = System.getProperty("user.home") + "/Downloads/" + fileName;

            if (type.equals("excel")) {
                DetailedPayrollExporter exporter = new DetailedPayrollExporter(selectedMonth, attendanceLog);
                exporter.exportToExcel(payrollTable, "Detailed Payroll", outputPath);
            } else {
                DetailedPayrollPdfExporter exporter = new DetailedPayrollPdfExporter(selectedMonth, attendanceLog);
                PdfWriter writer = new PdfWriter(outputPath);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);
                exporter.exportToPdf(document, payrollTable.getItems(), "Detailed Payroll");
                document.close();
            }

            System.out.println("Detailed " + type + " export completed: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAttendanceMonth(String selectedMonthYear) {
        try {
            Scene scene = rootPane.getScene();
            if (scene == null)
                return;

            Parent root = scene.getRoot();
            if (root == null)
                return;

            // Find AttendanceController in the current scene
            for (Node node : root.lookupAll("*")) {
                if (node.getId() != null && node.getId().equals("attendanceRoot")) {
                    Object controller = node.getProperties().get("controller");
                    if (controller instanceof AttendanceController attendanceController) {
                        attendanceController.setSelectedMonth(selectedMonthYear);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not sync month selection with Attendance: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
    }

    public void updateYear(String newYear) {
        initializeWithYear(newYear);
    }

    public void initializeWithYear(String year) {
        if (year == null) return;

        try {
            // Only need to update month combo box since filtering is handled by RootController
            if (yearMonthComboBox != null) {
                String currentValue = yearMonthComboBox.getValue();
                yearMonthComboBox.getItems().clear();
                DateTimeUtils.updateMonthYearComboBox(yearMonthComboBox, year);
                
                if (currentValue != null && yearMonthComboBox.getItems().contains(currentValue)) {
                    yearMonthComboBox.setValue(currentValue);
                }
            }

            payrollTable.refresh();
            updateTotalAmount();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSelectedMonth(String monthYear) {
        if (monthYear != null && yearMonthComboBox != null) {
            // Ensure the month exists in the combo box before setting it
            if (yearMonthComboBox.getItems().contains(monthYear)) {
                yearMonthComboBox.setValue(monthYear);
            }
            payrollTable.refresh();
            updateTotalAmount();
        }
    }

    public String getSelectedMonth() {
        return yearMonthComboBox != null ? yearMonthComboBox.getValue() : null;
    }
}