package sms.admin.app.attendance;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import atlantafx.base.controls.ModalPane;
import dev.finalproject.App;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.StackPane;
import sms.admin.app.RootController;
import sms.admin.app.attendance.dialog.AttendanceLogDialog;
import sms.admin.app.student.viewstudent.StudentProfileLoader;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.exporter.AttendanceTableExporter;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.SelectionMode;
import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
import dev.finalproject.data.StudentDAO; // Add this import

public class AttendanceController extends FXController {

    private static final String STUDENT_PROFILE_FXML = "/sms.admin/app/management/viewstudent/STUDENT_PROFILE.fxml";

    @FXML
    private ComboBox<String> monthYearComboBox;
    @FXML
    private TableView<Student> attendanceTable;
    @FXML
    private TableColumn<Student, Integer> colNo;
    @FXML
    private TableColumn<Student, String> colFullName;
    @FXML
    private TableColumn<Student, String> monthAttendanceColumn;
    @FXML
    private BorderPane rootPane;
    @FXML
    private Label currentDateLabel;
    @FXML
    private Label selectedStudentsLabel;
    @FXML
    private Label totalStudentsLabel;
    @FXML
    private ModalPane modalContainer;
    @FXML
    private StackPane dialogContainer;
    @FXML
    private MenuButton exportButton;
    @FXML
    private MenuItem exportExcel;
    @FXML
    private MenuItem exportCsv;
    @FXML
    private MenuItem exportPdf;
    // Remove viewAttendance MenuItem

    private ObservableList<Student> studentList;
    private ObservableList<AttendanceLog> attendanceLog;

    @Override
    protected void load_bindings() {
        // Comment out binding to test if manual visibility works
        // dialogContainer.visibleProperty().bind(modalContainer.displayProperty());
    }

    @Override
    protected void load_fields() {
        rootPane.getProperties().put("controller", this);
        studentList = App.COLLECTIONS_REGISTRY.getList("STUDENT");

        try {
            // Get attendance logs directly from DAO with validation
            List<AttendanceLog> dbLogs = AttendanceLogDAO.getAttendanceLogList().stream()
                    .filter(log -> {
                        if (log == null || log.getRecordID() == null) {
                            return false;
                        }
                        AttendanceRecord record = log.getRecordID();
                        return record.getMonth() >= 1 && record.getMonth() <= 12;
                    })
                    .collect(Collectors.toList());

            ObservableList<AttendanceLog> allLogs = FXCollections.observableArrayList(dbLogs);
            attendanceLog = allLogs.filtered(log -> !isFutureDate(log));
        } catch (Exception e) {
            System.err.println("Error loading attendance logs: " + e.getMessage());
            attendanceLog = FXCollections.observableArrayList();
        }

        setupTable();
        setupColumnWidths();

        String selectedYear = getSelectedYearOrDefault();
        initializeWithYear(selectedYear);

        // Get initially selected month from parameters
        String selectedMonth = (String) getParameter("selectedMonth");
        if (selectedMonth != null && monthYearComboBox.getItems().contains(selectedMonth)) {
            monthYearComboBox.setValue(selectedMonth);
        }

        setupMonthColumns();
        updateStudentCountLabels();
        // Remove viewAttendance initialization
    }

    private void setupTable() {
        colNo.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        colFullName.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            String fullName = String.format("%s, %s %s",
                    student.getLastName(),
                    student.getFirstName(),
                    student.getMiddleName());
            return new SimpleStringProperty(fullName);
        });

        attendanceTable.setItems(studentList);
        attendanceTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    // private TableCell<?, ?> findCell(int row, TableColumn<?, ?> column) {
    // for (Node node : attendanceTable.lookupAll(".table-cell")) {
    // if (node instanceof TableCell) {
    // @SuppressWarnings("unchecked")
    // TableCell<?, ?> cell = (TableCell<?, ?>) node;
    // if (cell.getTableRow() != null
    // && cell.getTableRow().getIndex() == row
    // && cell.getTableColumn() == column) {
    // return cell;
    // }
    // }
    // }
    // return null;
    // }

    private void setupColumnWidths() {
        // Fixed width for ID and Name columns
        colNo.setPrefWidth(44);
        colNo.setMinWidth(44);
        colNo.setMaxWidth(44);
        colNo.setResizable(false);

        colFullName.setPrefWidth(180);
        colFullName.setMinWidth(180);
        colFullName.setMaxWidth(180);
        colFullName.setResizable(false);

        // Make monthly attendance column take remaining space
        monthAttendanceColumn.setPrefWidth(-1); // Let it calculate automatically
    }

    private void setupMonthColumns() {
        monthAttendanceColumn.getColumns().clear();

        String selectedMonthYear = monthYearComboBox.getValue();
        if (selectedMonthYear == null) {
            return;
        }

        String[] parts = selectedMonthYear.split(" ");
        String monthName = parts[0];
        int yearNumber = Integer.parseInt(parts[1]);
        Month month = Month.valueOf(monthName.toUpperCase());

        LocalDate currentDate = LocalDate.of(yearNumber, month.getValue(), 1);
        LocalDate today = LocalDate.now();
        LocalDate endDate = (month.equals(today.getMonth()) && yearNumber == today.getYear())
                ? today // Stop at today if current month
                : currentDate.withDayOfMonth(currentDate.lengthOfMonth());

        TableColumn<Student, String> currentWeekColumn = new TableColumn<>("Week 1");
        currentWeekColumn.setStyle("-fx-alignment: CENTER;");
        monthAttendanceColumn.getColumns().add(currentWeekColumn);

        int weekNumber = 1;
        while (currentDate.isBefore(endDate.plusDays(1))) {
            if (currentDate.getDayOfWeek() == DayOfWeek.MONDAY
                    && !currentWeekColumn.getColumns().isEmpty()) {
                weekNumber++;
                currentWeekColumn = createWeekColumn(weekNumber);
                monthAttendanceColumn.getColumns().add(currentWeekColumn);
            }

            if (!AttendanceUtil.isWeekend(currentDate)) {
                final LocalDate cellDate = currentDate;
                TableColumn<Student, String> dayColumn = createDayColumn(cellDate);
                currentWeekColumn.getColumns().add(dayColumn);
            }

            currentDate = currentDate.plusDays(1);
        }

        Platform.runLater(this::adjustColumnWidths);
    }

    private void adjustColumnWidths() {
        // Get available width for weekly columns
        double availableWidth = attendanceTable.getWidth() - colNo.getWidth() - colFullName.getWidth() - 2;
        int weekCount = monthAttendanceColumn.getColumns().size();

        if (weekCount > 0) {
            // Distribute available width evenly among weeks
            double weekWidth = availableWidth / weekCount;

            monthAttendanceColumn.getColumns().forEach(weekCol -> {
                TableColumn<?, ?> column = (TableColumn<?, ?>) weekCol;
                int daysInWeek = column.getColumns().size();

                // Set week column to stretch
                column.setMinWidth(weekWidth);
                column.setPrefWidth(weekWidth);
                column.setMaxWidth(Double.MAX_VALUE);

                if (daysInWeek > 0) {
                    // Calculate width for each day
                    double dayWidth = (weekWidth - 4) / daysInWeek;

                    column.getColumns().forEach(dayCol -> {
                        TableColumn<?, ?> dc = (TableColumn<?, ?>) dayCol;
                        dc.setMinWidth(dayWidth);
                        dc.setPrefWidth(dayWidth);
                        dc.setMaxWidth(dayWidth);
                    });
                }
            });
        }

        // // Enable table to resize columns
        // attendanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @Override
    protected void load_listeners() {
        monthYearComboBox.setOnAction(event -> {
            if (!monthYearComboBox.isFocused()) {
                return; // Ignore programmatic changes
            }
            setupMonthColumns();
            // Update root controller with correct method name
            Scene scene = rootPane.getScene();
            if (scene != null) {
                Parent root = scene.getRoot();
                if (root != null) {
                    Object controller = root.getProperties().get("controller");
                    if (controller instanceof RootController rootController) {
                        rootController.setSelectedMonth(monthYearComboBox.getValue());
                    }
                }
            }
        });

        exportExcel.setOnAction(event -> handleExport("excel"));
        exportCsv.setOnAction(event -> handleExport("csv"));
        exportPdf.setOnAction(event -> handleExport("pdf"));
        // Remove viewAttendance listener

        attendanceTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateStudentCountLabels());

        attendanceTable.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() > 0) {
                Platform.runLater(this::adjustColumnWidths);
            }
        });
    }

    // private void updatePayrollMonth(String selectedMonthYear) {
    // try {
    // // Get root scene's FXML loader
    // Scene scene = rootPane.getScene();
    // if (scene == null)
    // return;

    // Parent root = scene.getRoot();
    // if (root == null)
    // return;

    // // Find PayrollController in the current scene
    // for (Node node : root.lookupAll("*")) {
    // if (node.getId() != null && node.getId().equals("payrollRoot")) {
    // Object controller = node.getProperties().get("controller");
    // if (controller instanceof PayrollController payrollController) {
    // payrollController.setSelectedMonth(selectedMonthYear);
    // return;
    // }
    // }
    // }
    // } catch (Exception e) {
    // System.err.println("Could not sync month selection with Payroll: " +
    // e.getMessage());
    // }
    // }

    private void handleExport(String type) {
        try {
            String selectedMonthYear = monthYearComboBox.getValue();
            if (selectedMonthYear == null) {
                return;
            }

            // Parse selected month and year
            String[] parts = selectedMonthYear.split(" ");
            String monthName = parts[0];
            int year = Integer.parseInt(parts[1]);
            Month month = Month.valueOf(monthName.toUpperCase());
            YearMonth selectedMonth = YearMonth.of(year, month.getValue());

            String title = "Attendance Report - " + selectedMonthYear;
            String fileName = String.format("attendance_%s.%s",
                    selectedMonthYear.replace(" ", "_").toLowerCase(),
                    type.equals("excel") ? "xlsx" : type.toLowerCase());
            String outputPath = System.getProperty("user.home") + "/Downloads/" + fileName;

            // Pass selected month to exporter
            AttendanceTableExporter exporter = new AttendanceTableExporter(selectedMonth);
            switch (type) {
                case "excel" ->
                    exporter.exportToExcel(attendanceTable, title, outputPath);
                case "pdf" ->
                    exporter.exportToPdf(attendanceTable, title, outputPath);
                case "csv" ->
                    exporter.exportToCsv(attendanceTable, title, outputPath);
            }

            System.out.println("Export completed: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TableColumn<Student, String> createWeekColumn(int weekNumber) {
        TableColumn<Student, String> weekColumn = new TableColumn<>("Week " + weekNumber);
        weekColumn.setStyle("-fx-alignment: CENTER;");
        weekColumn.setMinWidth(150);
        return weekColumn;
    }

    private TableColumn<Student, String> createDayColumn(LocalDate date) {
        TableColumn<Student, String> dayColumn = new TableColumn<>(date.getDayOfMonth() + "");
        dayColumn.setMinWidth(52);
        dayColumn.setPrefWidth(52);
        dayColumn.setMaxWidth(69);
        dayColumn.setResizable(false);
        dayColumn.setStyle("-fx-alignment: CENTER;");

        dayColumn.setCellValueFactory(data -> {
            Student student = data.getValue();
            String status = AttendanceUtil.getAttendanceStatus(student, date, attendanceLog);
            return new SimpleStringProperty(status);
        });

        dayColumn.setCellFactory(column -> {
            TableCell<Student, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || date.isAfter(LocalDate.now())) {
                        setText(null); // No display for future dates
                        setGraphic(null);
                    } else {
                        setText(item);
                    }
                }
            };

            if (!date.isAfter(LocalDate.now())) { // Only enable for non-future dates
                ContextMenu contextMenu = new ContextMenu();
                MenuItem viewAttendanceItem = new MenuItem("View Attendance Log");
                MenuItem editAttendanceItem = new MenuItem("Edit Attendance");

                viewAttendanceItem.setOnAction(e -> {
                    Student student = cell.getTableRow().getItem();
                    if (student != null) {
                        showAttendanceLogDialog(student, date);
                    }
                });

                editAttendanceItem.setOnAction(e -> {
                    Student student = cell.getTableRow().getItem();
                    if (student != null) {
                        String currentStatus = cell.getText();
                        ComboBox<String> comboBox = new ComboBox<>();
                        comboBox.getItems().addAll(
                                AttendanceUtil.PRESENT_MARK,
                                AttendanceUtil.ABSENT_MARK,
                                AttendanceUtil.HALF_DAY_MARK,
                                AttendanceUtil.EXCUSED_MARK);

                        comboBox.setValue(currentStatus.isEmpty() ? AttendanceUtil.PRESENT_MARK : currentStatus);
                        cell.setGraphic(comboBox);
                        cell.setText(null);

                        comboBox.setOnAction(event -> {
                            updateCellValue(cell, student, date, comboBox.getValue());
                        });

                        // Show combobox immediately
                        comboBox.show();
                    }
                });

                contextMenu.getItems().addAll(viewAttendanceItem, editAttendanceItem);
                cell.setContextMenu(contextMenu);
            }

            return cell;
        });

        return dayColumn;
    }

    private void updateCellValue(TableCell<Student, String> cell, Student student, LocalDate date, String newValue) {
        if (newValue != null) {
            if (newValue.equals(AttendanceUtil.EXCUSED_MARK)) {
                // Create excused attendance directly in database
                AttendanceRecord record = AttendanceRecordDAO.findOrCreateRecord(
                        date.getYear(), date.getMonthValue(), date.getDayOfMonth());

                AttendanceLog log = AttendanceLogDAO.findOrCreateLog(student, record);
                log.setTimeInAM(AttendanceUtil.TIME_EXCUSED);
                log.setTimeOutAM(AttendanceUtil.TIME_EXCUSED);
                log.setTimeInPM(AttendanceUtil.TIME_EXCUSED);
                log.setTimeOutPM(AttendanceUtil.TIME_EXCUSED);

                AttendanceLogDAO.update(log);
            } else {
                updateAttendanceRecord(student, date, newValue);
            }

            // Remove the ComboBox and update the cell text
            cell.setGraphic(null);
            cell.setText(newValue);

            Platform.runLater(() -> {
                // Clear any selection to prevent phantom highlights
                attendanceTable.getSelectionModel().clearSelection();

                // Refresh just the cell
                attendanceTable.refresh();

                // Clear focus to prevent highlight persistence
                attendanceTable.requestFocus();
            });
        }
    }

    private void showAttendanceLogDialog(Student student, LocalDate date) {
        try {
            // Create a new modifiable list for the dialog
            List<AttendanceLog> dialogLogs = new ArrayList<>(App.COLLECTIONS_REGISTRY.getList("ATTENDANCE_LOG"));
            AttendanceLogDialog dialog = new AttendanceLogDialog(student, date, dialogLogs);
            // Rest of the dialog setup...
        } catch (Exception e) {
            System.err.println("Error showing attendance log dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isFutureDate(AttendanceLog log) {
        try {
            if (log == null || log.getRecordID() == null) {
                return true;
            }

            AttendanceRecord record = log.getRecordID();
            int year = record.getYear();
            int month = record.getMonth();
            int day = record.getDay();

            // Validate date components
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                System.out.println("Invalid date components: year=" + year + ", month=" + month + ", day=" + day);
                return true;
            }

            LocalDate logDate = LocalDate.of(year, month, day);
            return logDate.isAfter(LocalDate.now());
        } catch (DateTimeException e) {
            System.err.println("Invalid date in attendance log: " + e.getMessage());
            return true;
        }
    }

    public void updateYear(String newYear) {
        initializeWithYear(newYear);
    }

    public void initializeWithYear(String year) {
        if (year == null) {
            return;
        }

        String[] yearRange = year.split("-");
        int startYear = Integer.parseInt(yearRange[0]);
        int endYear = Integer.parseInt(yearRange[1]);

        // Replace DataUtil with direct database query
        List<Student> students = StudentDAO.getStudentList().stream()
                .filter(student -> {
                    if (student.getYearID() == null) {
                        return false;
                    }
                    SchoolYear schoolYear = student.getYearID();
                    return schoolYear.getYearStart() == startYear
                            && schoolYear.getYearEnd() == endYear;
                })
                .collect(Collectors.toList());

        studentList = FXCollections.observableArrayList(students);
        attendanceTable.setItems(studentList);

        if (monthYearComboBox != null) {
            DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, year);

            YearMonth current = YearMonth.now();
            String currentFormatted = current.format(DateTimeUtils.MONTH_YEAR_FORMATTER);

            if (monthYearComboBox.getItems().contains(currentFormatted)) {
                monthYearComboBox.setValue(currentFormatted);
            } else if (!monthYearComboBox.getItems().isEmpty()) {
                monthYearComboBox.setValue(monthYearComboBox.getItems().get(0));
            }

            setupMonthColumns();
        }

        updateStudentCountLabels();
    }

    @FXML
    private void handleViewStudentButton() {
        initializeViewStudent();
    }

    private void initializeViewStudent() {
        StudentProfileLoader loader = (StudentProfileLoader) FXLoaderFactory
                .createInstance(StudentProfileLoader.class,
                        getClass().getResource(STUDENT_PROFILE_FXML))
                .initialize();
        loader.load();
    }

    private String getSelectedYearOrDefault() {
        String selectedYear = (String) getParameter("selectedYear");
        if (selectedYear == null) {
            LocalDate now = LocalDate.now();
            int year = now.getMonthValue() >= 6 ? now.getYear() : now.getYear() - 1;
            selectedYear = year + "-" + (year + 1);
        }
        return selectedYear;
    }

    private void updateStudentCountLabels() {
        if (studentList == null || attendanceTable == null
                || selectedStudentsLabel == null || totalStudentsLabel == null) {
            return;
        }

        int totalStudents = studentList.size();
        int selectedStudents = attendanceTable.getSelectionModel().getSelectedItems().size();

        selectedStudentsLabel.setText(String.format("Selected: %d", selectedStudents));
        totalStudentsLabel.setText(String.format("Total: %d", totalStudents));
    }

    // private LocalDate getDayColumnDate(String columnText) {
    // int day = Integer.parseInt(columnText.replaceAll("[MTWF]", ""));
    // String monthYear = monthYearComboBox.getValue();
    // String[] parts = monthYear.split(" ");
    // Month month = Month.valueOf(parts[0].toUpperCase());
    // int year = Integer.parseInt(parts[1]);
    // return LocalDate.of(year, month, day);
    // }

    private void updateAttendanceRecord(Student student, LocalDate date, String attendanceValue) {
        if (date.isAfter(LocalDate.now())) {
            return; // Prevent updates for future dates
        }

        // Create or get existing record
        AttendanceRecord record = AttendanceRecordDAO.findOrCreateRecord(date.getYear(),
                date.getMonthValue(), date.getDayOfMonth());

        // Create or get existing log
        AttendanceLog log = AttendanceLogDAO.findOrCreateLog(student, record);

        // Update log times based on attendance value
        updateLogTimes(log, attendanceValue);

        // Save to database
        AttendanceLogDAO.update(log);

        // Refresh local list
        refreshAttendanceLogs();
    }

    private void updateLogTimes(AttendanceLog log, String attendanceValue) {
        switch (attendanceValue) {
            case AttendanceUtil.PRESENT_MARK:
                log.setTimeInAM(AttendanceUtil.TIME_IN_AM);
                log.setTimeOutAM(AttendanceUtil.TIME_OUT_AM);
                log.setTimeInPM(AttendanceUtil.TIME_IN_PM);
                log.setTimeOutPM(AttendanceUtil.TIME_OUT_PM);
                break;
            case AttendanceUtil.ABSENT_MARK:
                log.setTimeInAM(AttendanceUtil.TIME_ABSENT);
                log.setTimeOutAM(AttendanceUtil.TIME_ABSENT);
                log.setTimeInPM(AttendanceUtil.TIME_ABSENT);
                log.setTimeOutPM(AttendanceUtil.TIME_ABSENT);
                break;
            case AttendanceUtil.HALF_DAY_MARK:
                log.setTimeInAM(AttendanceUtil.TIME_IN_AM);
                log.setTimeOutAM(AttendanceUtil.TIME_OUT_AM);
                log.setTimeInPM(AttendanceUtil.TIME_ABSENT);
                log.setTimeOutPM(AttendanceUtil.TIME_ABSENT);
                break;
            case AttendanceUtil.EXCUSED_MARK:
                log.setTimeInAM(AttendanceUtil.TIME_EXCUSED);
                log.setTimeOutAM(AttendanceUtil.TIME_EXCUSED);
                log.setTimeInPM(AttendanceUtil.TIME_EXCUSED);
                log.setTimeOutPM(AttendanceUtil.TIME_EXCUSED);
                break;
        }
    }

    private void refreshAttendanceLogs() {
        try {
            // Refresh the attendance logs from the database
            List<AttendanceLog> dbLogs = AttendanceLogDAO.getAttendanceLogList().stream()
                    .filter(log -> {
                        if (log == null || log.getRecordID() == null) {
                            return false;
                        }
                        AttendanceRecord record = log.getRecordID();
                        return record.getMonth() >= 1 && record.getMonth() <= 12;
                    })
                    .collect(Collectors.toList());

            // Update the attendanceLog observable list
            attendanceLog = FXCollections.observableArrayList(dbLogs)
                    .filtered(log -> !isFutureDate(log));

            // Refresh the main attendance table to show updated data
            Platform.runLater(() -> {
                attendanceTable.refresh();
            });
        } catch (Exception e) {
            System.err.println("Error refreshing attendance logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setSelectedMonth(String monthYear) {
        if (monthYearComboBox != null && monthYearComboBox.getItems().contains(monthYear)) {
            monthYearComboBox.setValue(monthYear);
            setupMonthColumns();
        }
    }
}