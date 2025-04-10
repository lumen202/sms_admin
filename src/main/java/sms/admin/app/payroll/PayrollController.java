package sms.admin.app.payroll;

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
import sms.admin.app.RootController;
import sms.admin.app.attendance.model.AttendanceSettings;
import sms.admin.app.payroll.dialog.PayrollExportDialogController;
import sms.admin.app.payroll.dialog.PayrollExportDialogLoader;
import sms.admin.util.attendance.CommonAttendanceUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.exporter.PayrollTableExporter;
import sms.admin.util.exporter.exporterv2.DetailedPayrollExporter;

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
    private MenuItem exportDetailedExcel;
    @FXML
    private RadioButton oneWayRadio;
    @FXML
    private RadioButton twoWayRadio;
    @FXML
    private RadioButton fourWayRadio;
    @FXML
    private ToggleGroup fareTypeGroup;

    private final DecimalFormat daysFormat = new DecimalFormat("#.# day(s)");
    private final DecimalFormat currencyFormat = new DecimalFormat("₱#,##0.00");
    private FilteredList<Student> filteredStudentList;
    private ObservableList<AttendanceLog> attendanceLog;
    private String currentYear; // Track the current year

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

        setupTable();
        updateTotalAmount();
    }

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
     * Updated to treat a holiday day as a full day present.
     */
    private double calculateTotalDays(Student student) {
        try {
            String monthYearValue = yearMonthComboBox.getValue();
            if (monthYearValue == null || monthYearValue.trim().isEmpty()) {
                return 0;
            }

            YearMonth selectedMonth = DateTimeUtils.parseMonthYear(monthYearValue);
            double totalDays = 0;

            List<AttendanceLog> studentLogs = attendanceLog.stream()
                    .filter(log -> log != null
                            && log.getStudentID() != null
                            && log.getStudentID().getStudentID() == student.getStudentID()
                            && log.getRecordID() != null
                            && YearMonth.of(log.getRecordID().getYear(), log.getRecordID().getMonth())
                                    .equals(selectedMonth)
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

        exportExcel.setOnAction(event -> handleExport("excel"));
        exportCsv.setOnAction(event -> handleExport("csv"));
        exportDetailedExcel.setOnAction(event -> handleExport("xlsx"));

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
            if (selectedMonthYear == null) {
                System.err.println("No month/year selected");
                return;
            }

            // Show export dialog
            PayrollExportDialogLoader dialogLoader = new PayrollExportDialogLoader(currentYear, selectedMonthYear,
                    type);
            dialogLoader.addParameter("OWNER_STAGE", rootPane.getScene().getWindow());
            dialogLoader.load();

            PayrollExportDialogController dialogController = dialogLoader.getController();
            if (dialogController == null || !dialogController.isConfirmed()) {
                System.out.println("Export cancelled by user");
                return;
            }

            // Get date range from dialog
            YearMonth startMonth = dialogController.getStartMonth();
            YearMonth endMonth = dialogController.getEndMonth();

            if (startMonth == null || endMonth == null) {
                System.err.println("Invalid date range selected");
                return;
            }

            // Continue with export...
            String title = String.format("Payroll Report - %s to %s",
                    startMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    endMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

            String fileName = String.format("payroll_%s_to_%s.%s",
                    startMonth.format(DateTimeFormatter.ofPattern("MMM_yyyy")).toLowerCase(),
                    endMonth.format(DateTimeFormatter.ofPattern("MMM_yyyy")).toLowerCase(),
                    type);

            String outputPath = System.getProperty("user.home") + "/Downloads/" + fileName;

            switch (type) {
                case "excel" -> handleBasicExport(type, title, outputPath, startMonth, endMonth);
                case "csv" -> handleBasicExport(type, title, outputPath, startMonth, endMonth);
                case "xlsx" -> handleDetailedExport("detailed-excel", title, outputPath, startMonth, endMonth);
            }

            System.out.println("Export completed: " + outputPath);
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleBasicExport(String type, String title, String outputPath, YearMonth startMonth,
            YearMonth endMonth) throws Exception {
        try {
            PayrollTableExporter exporter = new PayrollTableExporter();
            exporter.setFareMultiplier(getFareMultiplier());

            // Create consolidated payroll data for the date range
            List<Student> consolidatedData = consolidatePayrollData(startMonth, endMonth);

            switch (type) {
                case "excel" -> exporter.exportToExcel(payrollTable, title, outputPath, consolidatedData);
                case "csv" -> exporter.exportToCsv(payrollTable, title, outputPath, consolidatedData);
            }

            System.out.println("Export completed successfully: " + outputPath);
        } catch (Exception e) {
            System.err.println("Error during basic export: " + e.getMessage());
            e.printStackTrace();
            throw e; // Rethrow the exception to ensure it is handled upstream
        }
    }

    private void handleDetailedExport(String type, String title, String outputPath, YearMonth startMonth,
            YearMonth endMonth) throws Exception {
        try {
            YearMonth currentMonth = startMonth;
            while (!currentMonth.isAfter(endMonth)) {
                String monthTitle = String.format("Payroll Report - %s",
                        currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

                String monthFileName = String.format("payroll_%s.xlsx",
                        currentMonth.format(DateTimeFormatter.ofPattern("MMM_yyyy")).toLowerCase());

                String monthOutputPath = System.getProperty("user.home") + "/Downloads/" + monthFileName;

                DetailedPayrollExporter exporter = new DetailedPayrollExporter(currentMonth, currentMonth,
                        attendanceLog);
                exporter.setFareMultiplier(getFareMultiplier());
                exporter.exportToExcel(payrollTable, monthTitle, monthOutputPath);
                System.out.println("Detailed export completed successfully for "
                        + currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")) + ": " + monthOutputPath);

                currentMonth = currentMonth.plusMonths(1);
            }
        } catch (Exception e) {
            System.err.println("Error during detailed export: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private List<Student> consolidatePayrollData(YearMonth startMonth, YearMonth endMonth) {
        List<Student> consolidatedStudents = new ArrayList<>(filteredStudentList);
        PayrollTableExporter exporter = new PayrollTableExporter();
        exporter.setFareMultiplier(getFareMultiplier());

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

            // Store the total days in the exporter
            exporter.setConsolidatedDays(student.getStudentID(), totalDays);
        }

        return consolidatedStudents;
    }

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

    private void handleFareTypeChange(String fareType) {
        System.out.println("Selected fare type: " + fareType + " (multiplier: " + getFareMultiplier() + ")");
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

    public void updateYear(String newYear) {
        initializeWithYear(newYear);
    }

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

    public void setSelectedMonth(String monthYear) {
        if (monthYear != null && yearMonthComboBox != null) {
            if (!monthYear.equals(yearMonthComboBox.getValue()) && yearMonthComboBox.getItems().contains(monthYear)) {
                yearMonthComboBox.setValue(monthYear);
                payrollTable.refresh();
                updateTotalAmount();
            }
        }
    }

    public String getSelectedMonth() {
        return yearMonthComboBox != null ? yearMonthComboBox.getValue() : null;
    }

    /**
     * Added method: Returns the selected year or a default value.
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
