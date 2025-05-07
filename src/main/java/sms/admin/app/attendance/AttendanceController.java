package sms.admin.app.attendance;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import dev.finalproject.data.StudentDAO;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Button;
import sms.admin.app.attendance.dialog.AttendanceLogDialogLoader;
import sms.admin.util.attendance.AttendanceEditUtil;
import sms.admin.util.attendance.CommonAttendanceUtil;
import sms.admin.util.attendance.TableColumnUtil;
import sms.admin.util.attendance.WeeklyAttendanceUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.app.attendance.dialog.AttendanceSettingsDialogLoader;
import sms.admin.app.attendance.model.AttendanceSettings;

/**
 * Controller for the attendance view, managing the display and interaction with
 * attendance data.
 * This class handles the UI elements and logic for displaying student
 * attendance in a table,
 * allowing for editing, marking holidays, and exporting data.
 */
public class AttendanceController extends FXController {

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
    private Label selectedStudentsLabel;
    @FXML
    private Label totalStudentsLabel;
    @FXML
    private MenuButton exportButton;
    @FXML
    private MenuItem exportExcel, exportCsv;
    @FXML
    private Button settingsButton;
    private AttendanceSettings settings = new AttendanceSettings();

    private ObservableList<Student> studentList = FXCollections.observableArrayList();
    private final ObservableList<AttendanceLog> masterAttendanceLogs = FXCollections.observableArrayList();
    private final NavigableMap<LocalDate, Map<Integer, AttendanceLog>> dateToStudentLogs = new TreeMap<>();
    private boolean isMonthChanging = false;
    private String currentYear;

    /**
     * Loads the initial data and sets up the UI components.
     */
    @Override
    protected void load_fields() {
        rootPane.getProperties().put("controller", this);
        currentYear = getSelectedYearOrDefault();
        initializeStudentList(currentYear);

        if (!studentList.isEmpty()) {
            setupTable();
            loadAttendanceLogs();
            DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, currentYear);
            String defaultMonth = monthYearComboBox.getItems().get(0);
            String selectedMonth = (String) getParameter("selectedMonth");
            monthYearComboBox.setValue(selectedMonth != null ? selectedMonth : defaultMonth);
            setupMonthColumns();
            updateStudentCountLabels();
        }

        // Add listener to TableView's widthProperty for better resize handling
        attendanceTable.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() > 0) {
                double width = newWidth.doubleValue();
                System.out.println("TableView width changed to: " + width);
                TableColumnUtil.configureBasicColumns(colNo, colFullName, width);
                Platform.runLater(() -> {
                    setupMonthColumns();
                    attendanceTable.refresh();
                });
            }
        });
    }

    /**
     * Initializes the student list for the given academic year.
     *
     * @param year The academic year (e.g., "2023-2024").
     */
    private void initializeStudentList(String year) {
        try {
            int startYear = Integer.parseInt(year.split("-")[0]);
            List<Student> students = StudentDAO.getStudentList().stream()
                    .filter(s -> s != null && s.getYearID() != null
                            && s.getYearID().getYearStart() == startYear
                            && s.isDeleted() == 0)
                    .collect(Collectors.toList());

            studentList.setAll(students);
            attendanceTable.setItems(studentList);
        } catch (Exception e) {
            System.err.println("Error loading students: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads attendance logs for the current academic year and updates the UI.
     */
    private void loadAttendanceLogs() {
        try {
            masterAttendanceLogs.clear();
            dateToStudentLogs.clear();

            List<AttendanceLog> allLogs = DataManager.getInstance()
                    .getCollectionsRegistry()
                    .getList("ATTENDANCE_LOG");

            // Get student IDs from the current studentList (already filtered by year)
            List<Integer> studentIds = studentList.stream()
                    .map(Student::getStudentID)
                    .collect(Collectors.toList());

            // Define the academic year date range
            LocalDate startDate = getAcademicYearStartDate(currentYear);
            LocalDate endDate = getAcademicYearEndDate(currentYear);

            // Filter logs using a separate method
            List<AttendanceLog> filteredLogs = filterLogs(allLogs, studentIds, startDate, endDate);

            // Update UI on the JavaFX thread
            Platform.runLater(() -> {
                masterAttendanceLogs.setAll(filteredLogs);
                updateDateToStudentLogs();
                attendanceTable.refresh();
            });
        } catch (NullPointerException e) {
            System.err.println("Null reference encountered while loading attendance logs: " + e.getMessage());
            e.printStackTrace();
            handleEmptyLogs("Invalid data encountered");
        } catch (Exception e) {
            System.err.println("Unexpected error loading attendance logs: " + e.getMessage());
            e.printStackTrace();
            handleEmptyLogs("Error loading attendance logs");
        }
    }

    /**
     * Filters attendance logs based on student IDs, date range, and validity.
     *
     * @param allLogs    List of all attendance logs to filter
     * @param studentIds List of student IDs to include
     * @param startDate  Start of the academic year
     * @param endDate    End of the academic year
     * @return Filtered list of attendance logs
     */
    private List<AttendanceLog> filterLogs(List<AttendanceLog> allLogs, List<Integer> studentIds,
            LocalDate startDate, LocalDate endDate) {
        return allLogs.stream()
                .filter(log -> log != null && log.getStudentID() != null
                        && studentIds.contains(log.getStudentID().getStudentID()))
                .filter(log -> {
                    try {
                        LocalDate logDate = LocalDate.of(
                                log.getRecordID().getYear(),
                                log.getRecordID().getMonth(),
                                log.getRecordID().getDay());
                        return !logDate.isBefore(startDate) && !logDate.isAfter(endDate);
                    } catch (DateTimeException e) {
                        return false; // Exclude logs with invalid dates
                    }
                })
                .filter(log -> !isFutureDate(log)) // Exclude future dates
                .filter(this::isValidLog) // Exclude invalid logs
                .sorted((a, b) -> a.getLogID() - b.getLogID()) // Sort by log ID
                .collect(Collectors.toList());
    }

    /**
     * Gets the start date of the academic year.
     *
     * @param year The academic year (e.g., "2023-2024").
     * @return The start date (September 1st of the start year).
     */
    private LocalDate getAcademicYearStartDate(String year) {
        int startYear = Integer.parseInt(year.split("-")[0]);
        return LocalDate.of(startYear, 9, 1); // September 1st
    }

    /**
     * Gets the end date of the academic year.
     *
     * @param year The academic year (e.g., "2023-2024").
     * @return The end date (August 31st of the end year).
     */
    private LocalDate getAcademicYearEndDate(String year) {
        int endYear = Integer.parseInt(year.split("-")[1]);
        return LocalDate.of(endYear, 8, 31); // August 31st
    }

    /**
     * Handles the case when no logs are available or an error occurs.
     *
     * @param message The message to display or log.
     */
    private void handleEmptyLogs(String message) {
        Platform.runLater(() -> {
            masterAttendanceLogs.clear();
            dateToStudentLogs.clear();
            attendanceTable.refresh();
        });
    }

    /**
     * Checks if an attendance log is valid.
     *
     * @param log The attendance log to check.
     * @return true if the log is valid, false otherwise.
     */
    private boolean isValidLog(AttendanceLog log) {
        return log != null
                && log.getRecordID() != null
                && log.getStudentID() != null
                && log.getTimeInAM() >= -2
                && log.getTimeOutAM() >= -2
                && log.getTimeInPM() >= -2
                && log.getTimeOutPM() >= -2;
    }

    /**
     * Checks if the date of the attendance log is in the future.
     *
     * @param log The attendance log to check.
     * @return true if the date is in the future, false otherwise.
     */
    private boolean isFutureDate(AttendanceLog log) {
        if (log == null || log.getRecordID() == null) {
            return true;
        }
        try {
            LocalDate logDate = LocalDate.of(
                    log.getRecordID().getYear(),
                    log.getRecordID().getMonth(),
                    log.getRecordID().getDay());
            return logDate.isAfter(LocalDate.now());
        } catch (DateTimeException e) {
            return true;
        }
    }

    /**
     * Updates the date-to-student logs mapping for quick access.
     */
    private void updateDateToStudentLogs() {
        try {
            dateToStudentLogs.clear();
            for (AttendanceLog log : masterAttendanceLogs) {
                if (log == null || log.getRecordID() == null || log.getStudentID() == null) {
                    continue;
                }
                LocalDate date = LocalDate.of(
                        log.getRecordID().getYear(),
                        log.getRecordID().getMonth(),
                        log.getRecordID().getDay());
                dateToStudentLogs.computeIfAbsent(date, k -> new HashMap<>())
                        .put(log.getStudentID().getStudentID(), log);
            }
        } catch (Exception e) {
            System.err.println("Error mapping logs: " + e.getMessage());
        }
    }

    /**
     * Loads bindings for UI components, such as resizing with the window.
     */
    @Override
    protected void load_bindings() {
        // Use BorderPane's natural layout behavior
        attendanceTable.setMinWidth(800); // Set minimum width
        attendanceTable.setMinHeight(400); // Set minimum height
    }

    private double calculateAvailableWidth() {
        double tableWidth = attendanceTable.getWidth();
        double noWidth = colNo.getWidth();
        double nameWidth = colFullName.getWidth();
        double padding = 20;

        double available = Math.max(tableWidth - noWidth - nameWidth - padding, 400);
        System.out.println("Available width: " + available +
                " (Table: " + tableWidth +
                ", No: " + noWidth +
                ", Name: " + nameWidth + ")");
        return available;
    }

    /**
     * Loads event listeners for UI interactions.
     */
    @Override
    protected void load_listeners() {
        monthYearComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                setupMonthColumns();
            }
        });
        attendanceTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> updateStudentCountLabels());
        settingsButton.setOnAction(e -> showSettingsDialog());
    }

    /**
     * Sets up the attendance table with columns and configurations.
     */
    private void setupTable() {
        // Configure basic columns with responsive behavior
        colNo.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        colFullName.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue() != null
                        ? String.format("%s, %s %s",
                                c.getValue().getLastName(),
                                c.getValue().getFirstName(),
                                c.getValue().getMiddleName())
                        : ""));

        // Initialize responsive layout system
        TableColumnUtil.configureResponsiveLayout(
                attendanceTable,
                colNo,
                colFullName,
                monthAttendanceColumn);

        // Configure selection
        attendanceTable.setItems(studentList);
        attendanceTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Initial style application
        Platform.runLater(() -> attendanceTable.refresh());
    }

    /**
     * Sets up the month columns in the table based on the selected month.
     */
    private void setupMonthColumns() {
        if (isMonthChanging)
            return;
        isMonthChanging = true;

        monthAttendanceColumn.getColumns().clear();
        String monthYear = monthYearComboBox.getValue();
        if (monthYear == null) {
            isMonthChanging = false;
            return;
        }

        // Load settings for current month
        settings.loadForMonth(monthYear);

        LocalDate firstDayOfMonth = WeeklyAttendanceUtil.getFirstDayOfMonth(monthYear);
        LocalDate startDate = firstDayOfMonth.withDayOfMonth(settings.getStartDay());
        LocalDate endDate = firstDayOfMonth.withDayOfMonth(settings.getEndDay());

        List<WeeklyAttendanceUtil.WeekDates> allWeeks = WeeklyAttendanceUtil.splitIntoWeeks(startDate, endDate);

        // Use the new width calculation method
        double availableWidth = calculateAvailableWidth();
        int totalDays = allWeeks.stream().mapToInt(WeeklyAttendanceUtil::calculateWorkingDays).sum();
        double dayWidth = Math.max(30, availableWidth / Math.max(totalDays, 1));

        // Create columns
        AtomicInteger weekNum = new AtomicInteger(1);
        List<TableColumn<Student, String>> weekColumns = allWeeks.stream()
                .filter(WeeklyAttendanceUtil.WeekDates::hasWorkingDays)
                .map(week -> createWeekColumn(week, weekNum.getAndIncrement(), dayWidth))
                .collect(Collectors.toList());

        monthAttendanceColumn.getColumns().addAll(weekColumns);

        if (monthAttendanceColumn.getPrefWidth() == 0 || availableWidth > monthAttendanceColumn.getPrefWidth()) {
            monthAttendanceColumn.setPrefWidth(availableWidth);
        }
        TableColumnUtil.adjustColumnWidths(attendanceTable, colNo, colFullName, monthAttendanceColumn);
        Platform.runLater(() -> {
            attendanceTable.refresh();
            TableColumnUtil.updateColumnStyles(attendanceTable, 10);
        });
        isMonthChanging = false;
    }

    /**
     * Creates a week column for the table.
     *
     * @param week     The week dates.
     * @param weekNum  The week number.
     * @param dayWidth The width for each day column.
     * @return The week column.
     */
    private TableColumn<Student, String> createWeekColumn(WeeklyAttendanceUtil.WeekDates week, int weekNum,
            double dayWidth) {
        TableColumn<Student, String> weekColumn = new TableColumn<>("Week " + weekNum);
        weekColumn.setStyle("-fx-alignment: CENTER;");
        Map<DayOfWeek, List<LocalDate>> datesByDay = week.getDates().stream()
                .filter(d -> !CommonAttendanceUtil.isWeekend(d))
                .collect(Collectors.groupingBy(LocalDate::getDayOfWeek, TreeMap::new, Collectors.toList()));
        datesByDay.forEach((day, dates) -> {
            TableColumn<Student, String> dayColumn = new TableColumn<>(CommonAttendanceUtil.getDayInitial(day));
            dayColumn.setStyle("-fx-alignment: CENTER;");
            dates.sort(LocalDate::compareTo);
            dates.forEach(date -> {
                TableColumn<Student, String> dateColumn = createDayColumn(date, dayWidth);
                dateColumn.setText(String.valueOf(date.getDayOfMonth()));
                dayColumn.getColumns().add(dateColumn);
            });
            weekColumn.getColumns().add(dayColumn);
        });
        return weekColumn;
    }

    /**
     * Creates a day column for the table.
     *
     * @param date  The date for the column.
     * @param width The width of the column.
     * @return The day column.
     */
    private TableColumn<Student, String> createDayColumn(LocalDate date, double width) {
        TableColumn<Student, String> col = new TableColumn<>(String.valueOf(date.getDayOfMonth()));
        col.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            if (student == null || date.isAfter(LocalDate.now())) {
                return new SimpleStringProperty("-");
            }
            Map<Integer, AttendanceLog> logsForDate = dateToStudentLogs.get(date);
            if (logsForDate != null) {
                AttendanceLog log = logsForDate.get(student.getStudentID());
                if (log != null) {
                    String status = CommonAttendanceUtil.computeAttendanceStatus(log);
                    return new SimpleStringProperty(status);
                }
            }
            return new SimpleStringProperty(CommonAttendanceUtil.ABSENT_MARK);
        });
        col.setMinWidth(width);
        col.setPrefWidth(width);
        if (CommonAttendanceUtil.isHolidayDate(date)) {
            col.setStyle("-fx-background-color: lightcoral; -fx-alignment: CENTER;");
        } else {
            col.setStyle("-fx-alignment: CENTER;");
        }
        col.setCellFactory(c -> createDayCell(date));
        return col;
    }

    /**
     * Creates a custom table cell for attendance data.
     *
     * @param date The date for the cell.
     * @return The custom table cell.
     */
    private TableCell<Student, String> createDayCell(LocalDate date) {
        TableCell<Student, String> cell = new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setContextMenu(null);
                } else if (date.isAfter(LocalDate.now())) {
                    setText("-");
                    setStyle("-fx-text-fill: #999999; -fx-alignment: CENTER;");
                    setContextMenu(null);
                } else {
                    setText(item);
                    if (CommonAttendanceUtil.HOLIDAY_MARK.equals(item)) {
                        setStyle("-fx-background-color: lightcoral; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-alignment: CENTER;");
                    }
                    // Create context menu
                    ContextMenu menu = new ContextMenu();
                    MenuItem viewItem = new MenuItem("View Attendance Log");
                    viewItem.setOnAction(e -> showAttendanceLogDialog(getTableRow().getItem(), date));
                    menu.getItems().add(viewItem);

                    if (!CommonAttendanceUtil.HOLIDAY_MARK.equals(item)) {
                        MenuItem editItem = new MenuItem("Edit Attendance");
                        editItem.setOnAction(e -> editCell(this, date));
                        menu.getItems().add(editItem);
                        MenuItem holidayItem = new MenuItem("Mark as Holiday");
                        holidayItem.setOnAction(e -> markDayAsHoliday(date));
                        menu.getItems().add(holidayItem);
                    } else {
                        MenuItem unmarkItem = new MenuItem("Unmark as Holiday");
                        unmarkItem.setOnAction(e -> unmarkDayAsHoliday(date));
                        menu.getItems().add(unmarkItem);
                    }
                    setContextMenu(menu);
                }
            }
        };
        if (!date.isAfter(LocalDate.now())) {
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    editCell(cell, date);
                }
            });
        }
        return cell;
    }

    /**
     * Edits the attendance for a specific cell.
     *
     * @param cell The table cell to edit.
     * @param date The date of the attendance.
     */
    private void editCell(TableCell<Student, String> cell, LocalDate date) {
        Student student = cell.getTableRow().getItem();
        if (student == null || date.isAfter(LocalDate.now())) {
            return;
        }
        String currentStatus = cell.getText();
        if (CommonAttendanceUtil.HOLIDAY_MARK.equals(currentStatus)) {
            return;
        }
        AttendanceEditUtil.handleAttendanceEdit(cell, student, date, masterAttendanceLogs, updatedLog -> {
            if (updatedLog != null) {
                Platform.runLater(() -> {
                    DataManager.getInstance().refreshData();
                    loadAttendanceLogs();
                    setupMonthColumns(); // Refresh all columns
                });
            }
            cell.setGraphic(null);
        });
    }

    /**
     * Marks a day as a holiday for all students.
     *
     * @param date The date to mark as a holiday.
     */
    private void markDayAsHoliday(LocalDate date) {
        AttendanceEditUtil.markDayAsHoliday(date, studentList, masterAttendanceLogs, success -> {
            if (success) {
                Platform.runLater(() -> {
                    DataManager.getInstance().refreshData();
                    loadAttendanceLogs();
                    setupMonthColumns(); // Refresh all columns
                });
            }
        });
    }

    /**
     * Unmarks a day as a holiday.
     *
     * @param date The date to unmark as a holiday.
     */
    private void unmarkDayAsHoliday(LocalDate date) {
        AttendanceEditUtil.unmarkDayAsHoliday(date, masterAttendanceLogs, success -> {
            if (success) {
                Platform.runLater(() -> {
                    DataManager.getInstance().refreshData();
                    loadAttendanceLogs();
                    setupMonthColumns();
                });
            }
        });
    }

    /**
     * Shows the attendance log dialog for a specific student and date.
     *
     * @param student The student.
     * @param date    The date.
     */
    private void showAttendanceLogDialog(Student student, LocalDate date) {
        try {
            AttendanceLogDialogLoader loader = new AttendanceLogDialogLoader(student, date, masterAttendanceLogs);
            loader.load();
        } catch (Exception e) {
            System.err.println("Error showing attendance log dialog: " + e.getMessage());
        }
    }

    /**
     * Shows the settings dialog for attendance configuration.
     */
    private void showSettingsDialog() {
        try {
            System.out.println("Opening settings dialog with - Start: " + settings.getStartDay() + ", End: "
                    + settings.getEndDay());

            AttendanceSettingsDialogLoader loader = new AttendanceSettingsDialogLoader(settings);
            loader.addParameter("OWNER_STAGE", rootPane.getScene().getWindow());
            loader.addParameter("CURRENT_MONTH", monthYearComboBox.getValue());
            loader.load();

            // Listen for dialog close and refresh if needed
            loader.getController().getStage().setOnHidden(e -> {
                if (loader.getController().isSettingsChanged()) {
                    setupMonthColumns();
                    attendanceTable.refresh();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the labels showing the number of selected and total students.
     */
    private void updateStudentCountLabels() {
        if (studentList != null && attendanceTable != null) {
            selectedStudentsLabel.setText("Selected: " + attendanceTable.getSelectionModel().getSelectedItems().size());
            totalStudentsLabel.setText("Total: " + studentList.size());
        }
    }

    /**
     * Gets the selected academic year or defaults to the current year.
     *
     * @return The selected or default academic year.
     */
    private String getSelectedYearOrDefault() {
        String year = (String) getParameter("selectedYear");
        if (year == null) {
            int currentYear = LocalDate.now().getYear();
            year = (LocalDate.now().getMonthValue() >= 6 ? currentYear : currentYear - 1) + "-"
                    + (LocalDate.now().getMonthValue() >= 6 ? currentYear + 1 : currentYear);
        }
        return year;
    }

    /**
     * Gets the currently selected month.
     *
     * @return The selected month-year string.
     */
    public String getSelectedMonth() {
        return monthYearComboBox != null ? monthYearComboBox.getValue() : null;
    }

    /**
     * Gets the list of attendance logs.
     *
     * @return The observable list of attendance logs.
     */
    public ObservableList<AttendanceLog> getAttendanceLogs() {
        return masterAttendanceLogs;
    }

    /**
     * Initializes the controller with a specific academic year.
     *
     * @param year The academic year to initialize with.
     */
    public void initializeWithYear(String year) {
        if (year == null || year.equals(currentYear)) {
            return;
        }
        currentYear = year;
        initializeStudentList(year);
        loadAttendanceLogs();
        DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, year);
        String defaultMonth = monthYearComboBox.getItems().get(0);
        String selectedMonth = (String) getParameter("selectedMonth");
        monthYearComboBox.setValue(selectedMonth != null ? selectedMonth : defaultMonth);
        setupMonthColumns();
        updateStudentCountLabels();
        Platform.runLater(() -> attendanceTable.refresh());
    }

    /**
     * Sets the selected month in the combo box.
     *
     * @param monthYear The month-year string to select.
     */
    public void setSelectedMonth(String monthYear) {
        if (monthYear != null && monthYearComboBox != null && monthYearComboBox.getItems().contains(monthYear)) {
            Platform.runLater(() -> {
                if (!monthYear.equals(monthYearComboBox.getValue())) {
                    monthYearComboBox.setValue(monthYear);
                    setupMonthColumns();
                }
            });
        }
    }

    /**
     * Refreshes the attendance view by reloading the data
     */
    public void refreshView() {
        Platform.runLater(() -> {
            DataManager.getInstance().refreshData();
            attendanceTable.refresh();
        });
    }
}
