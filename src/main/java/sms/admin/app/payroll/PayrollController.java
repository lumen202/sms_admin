package sms.admin.app.payroll;

import java.text.DecimalFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

import com.itextpdf.layout.Document;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
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
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.exporter.PayrollTableExporter;
import sms.admin.util.exporter.exporterv2.DetailedPayrollExporter;
import sms.admin.util.exporter.exporterv2.DetailedPayrollPdfExporter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

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

    // Add new fields
    @FXML private RadioButton oneWayRadio;
    @FXML private RadioButton twoWayRadio;
    @FXML private RadioButton fourWayRadio;
    @FXML private ToggleGroup fareTypeGroup;

    private Map<Integer, List<AttendanceLog>> studentAttendanceCache = new HashMap<>();

    private final DecimalFormat daysFormat = new DecimalFormat("#.# day(s)");
    private final DecimalFormat currencyFormat = new DecimalFormat("₱#,##0.00");

    private FilteredList<Student> filteredStudentList;
    private ObservableList<AttendanceLog> attendanceLog;

    @SuppressWarnings("unchecked")
    @Override
    protected void load_fields() {
        rootPane.getProperties().put("controller", this);
        filteredStudentList = (FilteredList<Student>) getParameter("filteredStudentList");
        
        // Set four-way as default
        if (fourWayRadio != null) {
            fourWayRadio.setSelected(true);
        }

        // Initialize month selection
        String selectedYear = (String) getParameter("selectedYear");
        String selectedMonth = (String) getParameter("selectedMonth");
        
        if (selectedYear != null) {
            DateTimeUtils.updateMonthYearComboBox(yearMonthComboBox, selectedYear);
            if (selectedMonth != null && yearMonthComboBox.getItems().contains(selectedMonth)) {
                yearMonthComboBox.setValue(selectedMonth);
            }
        }

        setupTable();
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

        // Update fare formatting to show multiplied fare
        colFare.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            double multipliedFare = student.getFare() * getFareMultiplier();
            return new SimpleDoubleProperty(multipliedFare).asObject();
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

        // Update total amount calculation to use the already multiplied fare
        colTotalAmount.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            double totalDays = calculateTotalDays(student);
            double multipliedFare = student.getFare() * getFareMultiplier();
            return new SimpleDoubleProperty(totalDays * multipliedFare).asObject();
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

        // Add fixed widths to prevent column resizing issues
        colNo.setPrefWidth(80);
        colNo.setMinWidth(80);
        colNo.setMaxWidth(80);
        
        colFullName.setPrefWidth(220);
        colFullName.setMinWidth(220);
        
        colTotalDays.setPrefWidth(100);
        colTotalDays.setMinWidth(100);
        
        colFare.setPrefWidth(200);
        colFare.setMinWidth(200);
        
        colTotalAmount.setPrefWidth(120);
        colTotalAmount.setMinWidth(120);
    }

    private void updateTotalAmount() {
        double totalAmount = filteredStudentList.stream()
                .mapToDouble(student -> {
                    double totalDays = calculateTotalDays(student);
                    double fare = student.getFare() * getFareMultiplier();
                    return totalDays * fare;
                })
                .sum();

        totalAmountLabel.setText(currencyFormat.format(totalAmount));
    }

    @SuppressWarnings("unchecked")
    private double calculateTotalDays(Student student) {
        try {
            String monthYearValue = yearMonthComboBox.getValue();
            if (monthYearValue == null || monthYearValue.trim().isEmpty()) {
                return 0;
            }

            YearMonth selectedMonth = DateTimeUtils.parseMonthYear(monthYearValue);
            double totalDays = 0;

            // Use the shared observable list instead of parameter
            List<AttendanceLog> studentLogs = AttendanceController.CURRENT_LOGS.stream()
                .filter(log -> log != null && 
                       log.getStudentID() != null && 
                       log.getStudentID().getStudentID() == student.getStudentID())
                .collect(Collectors.toList());

            // Process logs for selected month
            for (AttendanceLog log : studentLogs) {
                if (log.getRecordID() == null) continue;
                
                AttendanceRecord record = log.getRecordID();
                YearMonth logMonth = YearMonth.of(record.getYear(), record.getMonth());
                
                if (logMonth.equals(selectedMonth)) {
                    // Calculate days based on status
                    if (isAbsent(log)) {
                        continue;
                    } else if (isExcused(log)) {
                        totalDays += 1.0;
                    } else if (isHalfDay(log)) {
                        totalDays += 0.5;
                    } else {
                        totalDays += 1.0;
                    }
                }
            }

            return totalDays;
        } catch (Exception e) {
            System.err.println("Error calculating days for student " + student.getStudentID());
            e.printStackTrace();
            return 0;
        }
    }

    private boolean isAbsent(AttendanceLog log) {
        return log.getTimeInAM() == AttendanceUtil.TIME_ABSENT && 
               log.getTimeOutAM() == AttendanceUtil.TIME_ABSENT &&
               log.getTimeInPM() == AttendanceUtil.TIME_ABSENT && 
               log.getTimeOutPM() == AttendanceUtil.TIME_ABSENT;
    }

    private boolean isExcused(AttendanceLog log) {
        return log.getTimeInAM() == AttendanceUtil.TIME_EXCUSED;
    }

    private boolean isHalfDay(AttendanceLog log) {
        return log.getTimeInPM() == AttendanceUtil.TIME_ABSENT && 
               log.getTimeOutPM() == AttendanceUtil.TIME_ABSENT;
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
        
        // Add size bindings to prevent table movement
        if (payrollTable != null) {
            payrollTable.prefWidthProperty().bind(rootPane.widthProperty().subtract(40)); // 20px padding on each side
            payrollTable.prefHeightProperty().bind(rootPane.heightProperty().subtract(200)); // Adjust for header and footer
        }
    }

    @Override
    protected void load_listeners() {
        yearMonthComboBox.setOnAction(event -> {
            if (!yearMonthComboBox.isFocused()) return;
            String newValue = yearMonthComboBox.getValue();
            System.out.println("PayrollController: Month changed to " + newValue);
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

        // Add radio button listeners
        fareTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                RadioButton selected = (RadioButton) newVal;
                handleFareTypeChange(selected.getText());
            }
        });
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

    private void handleFareTypeChange(String fareType) {
        System.out.println("Selected fare type: " + fareType + " (multiplier: " + getFareMultiplier() + ")");
        
        // Just refresh the cell values without toggling column visibility
        Platform.runLater(() -> {
            payrollTable.refresh();
            updateTotalAmount();
        });
    }

    private double getFareMultiplier() {
        if (fourWayRadio.isSelected()) {
            return 4.0;
        } else if (twoWayRadio.isSelected()) {
            return 2.0;
        }
        return 1.0; // one way
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
            if (!monthYear.equals(yearMonthComboBox.getValue()) && 
                yearMonthComboBox.getItems().contains(monthYear)) {
                yearMonthComboBox.setValue(monthYear);
                payrollTable.refresh();
                updateTotalAmount();
            }
        }
    }

    public String getSelectedMonth() {
        return yearMonthComboBox != null ? yearMonthComboBox.getValue() : null;
    }

    // Update refreshData to properly sync with attendance logs
    @SuppressWarnings("unchecked")
    public void refreshData() {
        Platform.runLater(() -> {
            try {
                filteredStudentList = (FilteredList<Student>) getParameter("filteredStudentList");
                payrollTable.setItems(filteredStudentList);
                payrollTable.refresh();
                updateTotalAmount();
                
                System.out.println("PayrollController: Refreshed with " + filteredStudentList.size() + " students");
            } catch (Exception e) {
                System.err.println("Error refreshing payroll data: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

}