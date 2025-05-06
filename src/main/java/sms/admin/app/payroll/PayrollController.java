package sms.admin.app.payroll;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import sms.admin.app.RootController;
import sms.admin.app.attendance.model.AttendanceSettings;
import sms.admin.app.payroll.dialog.PayrollExportDialogController;
import sms.admin.app.payroll.dialog.PayrollExportDialogLoader;
import sms.admin.util.attendance.CommonAttendanceUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.exporter.StudentTableExporter;
import sms.admin.util.exporter.TableDataProvider;
import sms.admin.util.exporter.exporterv2.DetailedPayrollExporter;

/**
 * Controller class for managing payroll operations and UI interactions.
 * Handles student attendance tracking, fare calculations, and report
 * generation.
 */
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
    private MenuItem exportCsv;
    @FXML
    private MenuItem exportDetailedExcel;
    @FXML
    private RadioButton oneWayRadio;
    @FXML
    private RadioButton twoWayRadio;
    @FXML
    private RadioButton fourWayRadio;
    @FXML
    private ToggleGroup fareTypeGroup;

    /** Formats for displaying days with proper unit */
    private final DecimalFormat daysFormat = new DecimalFormat("#.# day(s)");

    /** Formats currency values with peso symbol */
    private final DecimalFormat currencyFormat = new DecimalFormat("₱#,##0.00");

    /** Filtered list of students for the current academic year */
    private FilteredList<Student> filteredStudentList;

    /** Collection of attendance logs for calculations */
    private ObservableList<AttendanceLog> attendanceLog;

    /** Current academic year in format "YYYY-YYYY" */
    private String currentYear;

    private TableDataProvider<Student> exporter;

    /**
     * Initializes the controller and loads initial data.
     * Sets up the UI components and default values.
     */
    @Override
    protected void load_fields() {
        rootPane.getProperties().put("controller", this);
        currentYear = getSelectedYearOrDefault();
        initializeData(currentYear);

        if (fourWayRadio != null) {
            fourWayRadio.setSelected(true); // Set four-way as default
        }

        DateTimeUtils.updateMonthYearComboBox(yearMonthComboBox, currentYear);
        String selectedMonth = (String) getParameter("selectedMonth");
        if (selectedMonth != null && yearMonthComboBox.getItems().contains(selectedMonth)) {
            yearMonthComboBox.setValue(selectedMonth);
        } else {
            yearMonthComboBox.setValue(yearMonthComboBox.getItems().get(0));
        }

        exporter = new StudentTableExporter(); // Initialize exporter
        setupTable();
        updateTotalAmount();
    }

    /**
     * Initializes data for the specified academic year.
     * Loads students and attendance logs from the data manager.
     * 
     * @param year Academic year in format "YYYY-YYYY"
     */
    @SuppressWarnings("unchecked")
    private void initializeData(String year) {
        int startYear = Integer.parseInt(year.split("-")[0]);
        // Retrieve the student list from DataManager instead of direct DAO call
        List<Student> students = DataManager.getInstance().getCollectionsRegistry().getList("STUDENT");
        filteredStudentList = new FilteredList<>(FXCollections.observableArrayList(
                students.stream()
                        .filter(student -> student.getYearID() != null
                                && student.getYearID().getYearStart() == startYear && student.isDeleted() == 0)
                        .collect(Collectors.toList())));

        // Initialize attendance logs from shared DataManager if not provided as a
        // parameter.
        attendanceLog = (ObservableList<AttendanceLog>) getParameter("attendanceLogs");
        if (attendanceLog == null) {
            attendanceLog = DataManager.getInstance().getCollectionsRegistry().getList("ATTENDANCE_LOG");
        }
        payrollTable.setItems(filteredStudentList);
    }

    /**
     * Updates the root controller with the selected month.
     * Enables synchronization between different views.
     * 
     * @param monthYear Selected month in format "MMMM yyyy"
     */
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

    /**
     * Sets up the table columns and their cell factories.
     * Configures how data is displayed and formatted in the table.
     */
    private void setupTable() {
        colNo.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        colFullName.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            String fullName = String.format("%s, %s %s", student.getLastName(), student.getFirstName(),
                    student.getMiddleName());
            return new SimpleStringProperty(fullName);
        });

        colTotalDays.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            double totalDays = calculateTotalDays(student);
            return new SimpleIntegerProperty((int) (totalDays * 10)).asObject();
        });
        colTotalDays.setCellFactory(column -> new TableCell<Student, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    double days = item / 10.0;
                    setText(daysFormat.format(days));
                }
            }
        });

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

        payrollTable.setItems(filteredStudentList);
    }

    /**
     * Updates the total amount label with the sum of all student payments.
     * Calculates based on attendance days and fare rates.
     */
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

    /**
     * Calculates total attendance days for a student in the selected month.
     * Uses DetailedPayrollExporter for consistent calculations.
     * 
     * @param student Student to calculate days for
     * @return Total number of days present (including half days)
     */
    private double calculateTotalDays(Student student) {
        try {
            String monthYearValue = yearMonthComboBox.getValue();
            if (monthYearValue == null || monthYearValue.trim().isEmpty()) {
                return 0;
            }

            YearMonth selectedMonth = DateTimeUtils.parseMonthYear(monthYearValue);
            return new DetailedPayrollExporter(selectedMonth, selectedMonth, attendanceLog)
                    .calculateStudentDays(student, selectedMonth);
        } catch (Exception e) {
            System.err.println("Error calculating days for student " + student.getStudentID());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Sets up UI element bindings for responsive layout.
     * Ensures proper resizing of components.
     */
    @Override
    protected void load_bindings() {
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                rootPane.prefWidthProperty().bind(newScene.widthProperty());
                rootPane.prefHeightProperty().bind(newScene.heightProperty());
            }
        });
        payrollTable.prefWidthProperty().bind(rootPane.widthProperty().subtract(40));
        payrollTable.prefHeightProperty().bind(rootPane.heightProperty().subtract(200));
    }

    /**
     * Sets up event listeners for UI interactions.
     * Handles month selection, export actions, and fare type changes.
     */
    @Override
    protected void load_listeners() {
        yearMonthComboBox.setOnAction(event -> {
            if (!yearMonthComboBox.isFocused()) {
                return;
            }
            String newValue = yearMonthComboBox.getValue();
            System.out.println("PayrollController: Month changed to " + newValue);
            payrollTable.refresh();
            updateTotalAmount();
            updateRootController(newValue);
        });

        exportCsv.setOnAction(event -> handleExport("csv"));
        exportDetailedExcel.setOnAction(event -> handleExport("xlsx"));

        fareTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                RadioButton selected = (RadioButton) newVal;
                handleFareTypeChange(selected.getText());
            }
        });
    }

    /**
     * Handles the export process for different file formats.
     * Supports CSV and Excel (XLSX) exports with date range selection.
     * 
     * @param type Export file type ("csv" or "xlsx")
     */
    private void handleExport(String type) {
        try {
            String selectedMonthYear = yearMonthComboBox.getValue();
            if (selectedMonthYear == null) {
                System.err.println("No month/year selected");
                return;
            }

            // Get date range from dialog
            PayrollExportDialogLoader dialogLoader = new PayrollExportDialogLoader(currentYear, selectedMonthYear,
                    type);
            dialogLoader.addParameter("OWNER_STAGE", rootPane.getScene().getWindow());
            dialogLoader.load();

            PayrollExportDialogController dialogController = dialogLoader.getController();
            if (dialogController == null || !dialogController.isConfirmed()) {
                System.out.println("Export cancelled by user");
                return;
            }

            YearMonth startMonth = dialogController.getStartMonth();
            YearMonth endMonth = dialogController.getEndMonth();

            File outputDir = showSaveDirectory();
            if (outputDir == null)
                return;

            // Export for each month in the range
            YearMonth currentMonth = startMonth;
            while (!currentMonth.isAfter(endMonth)) {
                String fileName = String.format("payroll_%s.%s",
                        currentMonth.format(DateTimeFormatter.ofPattern("MMM_yyyy")).toLowerCase(),
                        type);
                File file = new File(outputDir, fileName);

                String title = String.format("Payroll Report - %s",
                        currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

                if ("xlsx".equals(type)) {
                    DetailedPayrollExporter detailedExporter = new DetailedPayrollExporter(currentMonth, currentMonth,
                            attendanceLog);
                    detailedExporter.setFareMultiplier(getFareMultiplier());
                    detailedExporter.exportToExcel(payrollTable, title, file.getAbsolutePath());
                } else {
                    try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
                        exporter.writeDataToCsv(writer, payrollTable.getItems(), title);
                    }
                }

                currentMonth = currentMonth.plusMonths(1);
            }

            showSuccessAlert("Export Complete", "Successfully exported payroll reports",
                    outputDir.getAbsolutePath());

            System.out.println("Export completed to directory: " + outputDir.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Export Error",
                    "Failed to export payroll to " + type.toUpperCase(),
                    "Error: " + e.getMessage());
        }
    }

    private File showSaveDirectory() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Export Directory");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Downloads"));
        return dirChooser.showDialog(rootPane.getScene().getWindow());
    }

    /**
     * Shows a success alert dialog with the specified details.
     *
     * @param title   The title of the alert
     * @param header  The header text of the alert
     * @param content The content text of the alert
     */
    private void showSuccessAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content); // Content already contains full path
        alert.showAndWait();
    }

    /**
     * Shows an error alert dialog with the specified details.
     *
     * @param title   The title of the alert
     * @param header  The header text of the alert
     * @param content The content text of the alert
     */
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Consolidates payroll data across a date range.
     * Aggregates attendance and calculates totals for each student.
     * 
     * @param startMonth Start of date range
     * @param endMonth   End of date range
     * @return List of students with consolidated data
     */
    private List<Student> consolidatePayrollData(YearMonth startMonth, YearMonth endMonth) {
        List<Student> consolidatedStudents = new ArrayList<>(filteredStudentList);

        // For each student, accumulate attendance across the date range
        for (Student student : consolidatedStudents) {
            double totalDays = 0;
            YearMonth currentMonth = startMonth;

            while (!currentMonth.isAfter(endMonth)) {
                // Load attendance settings for the month
                AttendanceSettings settings = new AttendanceSettings();
                settings.loadForMonth(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

                // Calculate days for the month within settings range
                double monthDays = calculateTotalDaysInRange(student, currentMonth, settings);
                totalDays += monthDays;

                currentMonth = currentMonth.plusMonths(1);
            }
        }

        return consolidatedStudents;
    }

    /**
     * Calculates total days within a specific month and settings range.
     * Considers attendance status and weekend exclusions.
     * 
     * @param student  Student to calculate for
     * @param month    Month to calculate within
     * @param settings Attendance settings for the month
     * @return Total days present
     */
    private double calculateTotalDaysInRange(Student student, YearMonth month, AttendanceSettings settings) {
        try {
            double totalDays = 0;
            int startDay = settings.getStartDay();
            int endDay = settings.getEndDay();

            List<AttendanceLog> studentLogs = attendanceLog.stream()
                    .filter(log -> log != null
                            && log.getStudentID() != null
                            && log.getStudentID().getStudentID() == student.getStudentID()
                            && log.getRecordID() != null
                            && log.getRecordID().getYear() == month.getYear()
                            && log.getRecordID().getMonth() == month.getMonthValue()
                            && log.getRecordID().getDay() >= startDay
                            && log.getRecordID().getDay() <= endDay
                            && !CommonAttendanceUtil.isWeekend(LocalDate.of(
                                    log.getRecordID().getYear(),
                                    log.getRecordID().getMonth(),
                                    log.getRecordID().getDay())))
                    .collect(Collectors.toList());

            for (AttendanceLog log : studentLogs) {
                String status = CommonAttendanceUtil.computeAttendanceStatus(log);
                switch (status) {
                    case CommonAttendanceUtil.PRESENT_MARK,
                            CommonAttendanceUtil.EXCUSED_MARK,
                            CommonAttendanceUtil.HOLIDAY_MARK ->
                        totalDays += 1.0;
                    case CommonAttendanceUtil.HALF_DAY_MARK -> totalDays += 0.5;
                }
            }
            return totalDays;
        } catch (Exception e) {
            System.err.println("Error calculating days for student " + student.getStudentID());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Handles fare type change and updates the table accordingly.
     * Refreshes the table and recalculates total amounts.
     * 
     * @param fareType Selected fare type
     */
    private void handleFareTypeChange(String fareType) {
        System.out.println("Selected fare type: " + fareType + " (multiplier: " + getFareMultiplier() + ")");
        Platform.runLater(() -> {
            payrollTable.refresh();
            updateTotalAmount();
        });
    }

    /**
     * Returns the fare multiplier based on the selected fare type.
     * 
     * @return Fare multiplier (1.0, 2.0, or 4.0)
     */
    private double getFareMultiplier() {
        if (fourWayRadio.isSelected()) {
            return 4.0;
        } else if (twoWayRadio.isSelected()) {
            return 2.0;
        }
        return 1.0; // one way
    }

    /**
     * Updates the controller with a new academic year.
     * 
     * @param newYear New academic year in format "YYYY-YYYY"
     */
    public void updateYear(String newYear) {
        initializeWithYear(newYear);
    }

    /**
     * Initializes the controller with a specific academic year.
     * 
     * @param year Academic year in format "YYYY-YYYY"
     */
    public void initializeWithYear(String year) {
        if (year == null || year.equals(currentYear)) {
            return;
        }
        currentYear = year;
        initializeData(year);
        DateTimeUtils.updateMonthYearComboBox(yearMonthComboBox, year);
        String selectedMonth = (String) getParameter("selectedMonth");
        if (selectedMonth != null && yearMonthComboBox.getItems().contains(selectedMonth)) {
            yearMonthComboBox.setValue(selectedMonth);
        } else {
            yearMonthComboBox.setValue(yearMonthComboBox.getItems().get(0));
        }
        payrollTable.refresh();
        updateTotalAmount();
    }

    /**
     * Sets the selected month in the combo box.
     * 
     * @param monthYear Month in format "MMMM yyyy"
     */
    public void setSelectedMonth(String monthYear) {
        if (monthYear != null && yearMonthComboBox != null) {
            if (!monthYear.equals(yearMonthComboBox.getValue()) && yearMonthComboBox.getItems().contains(monthYear)) {
                yearMonthComboBox.setValue(monthYear);
                payrollTable.refresh();
                updateTotalAmount();
            }
        }
    }

    /**
     * Gets the currently selected month from the combo box.
     * 
     * @return Selected month in format "MMMM yyyy"
     */
    public String getSelectedMonth() {
        return yearMonthComboBox != null ? yearMonthComboBox.getValue() : null;
    }

    /**
     * Returns the selected year or a default value.
     * 
     * @return Selected year in format "YYYY-YYYY"
     */
    private String getSelectedYearOrDefault() {
        String year = (String) getParameter("selectedYear");
        if (year == null) {
            int currentYear = java.time.LocalDate.now().getYear();
            year = (java.time.LocalDate.now().getMonthValue() >= 6 ? currentYear : currentYear - 1) + "-"
                    + (java.time.LocalDate.now().getMonthValue() >= 6 ? currentYear + 1 : currentYear);
        }
        return year;
    }
}
