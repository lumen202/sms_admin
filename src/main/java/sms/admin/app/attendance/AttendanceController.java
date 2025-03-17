package sms.admin.app.attendance;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import atlantafx.base.controls.ModalPane;
import dev.finalproject.App;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
import dev.finalproject.data.StudentDAO;
import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import javafx.scene.layout.StackPane;
import sms.admin.app.RootController;
import sms.admin.app.attendance.dialog.AttendanceLogDialog;
import sms.admin.app.student.viewstudent.StudentProfileLoader;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.attendance.AttendanceDateUtil;
import sms.admin.util.attendance.WeeklyAttendanceUtil;
import sms.admin.util.attendance.TableColumnUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.exporter.AttendanceTableExporter;

public class AttendanceController extends FXController {

    private static final String STUDENT_PROFILE_FXML = "/sms.admin/app/management/viewstudent/STUDENT_PROFILE.fxml";

    @FXML private ComboBox<String> monthYearComboBox;
    @FXML private TableView<Student> attendanceTable;
    @FXML private TableColumn<Student, Integer> colNo;
    @FXML private TableColumn<Student, String> colFullName;
    @FXML private TableColumn<Student, String> monthAttendanceColumn;
    @FXML private BorderPane rootPane;
    @FXML private Label currentDateLabel;
    @FXML private Label selectedStudentsLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private ModalPane modalContainer;
    @FXML private StackPane dialogContainer;
    @FXML private MenuButton exportButton;
    @FXML private MenuItem exportExcel;
    @FXML private MenuItem exportCsv;
    @FXML private MenuItem exportPdf;
    
    private ObservableList<Student> studentList;
    private ObservableList<AttendanceLog> attendanceLog;

    private Map<String, AttendanceLog> logCache = new HashMap<>();
    private Map<LocalDate, AttendanceRecord> recordCache = new HashMap<>();

    @Override
    protected void load_bindings() {
        // Optionally, you can bind dialogContainer's visibility to modalContainer's display property.
        // dialogContainer.visibleProperty().bind(modalContainer.displayProperty());
    }

    @Override
    protected void load_fields() {
        rootPane.getProperties().put("controller", this);
        studentList = App.COLLECTIONS_REGISTRY.getList("STUDENT");
        loadAttendanceLogs();

        setupTable();
        setupColumnWidths();

        String selectedYear = getSelectedYearOrDefault();
        initializeWithYear(selectedYear);

        // Set the initially selected month if provided in parameters.
        String selectedMonth = (String) getParameter("selectedMonth");
        if (selectedMonth != null && monthYearComboBox.getItems().contains(selectedMonth)) {
            monthYearComboBox.setValue(selectedMonth);
        }

        setupMonthColumns();
        updateStudentCountLabels();
    }

    private void loadAttendanceLogs() {
        try {
            List<AttendanceLog> dbLogs = AttendanceLogDAO.getAttendanceLogList().stream()
                .filter(log -> log != null && log.getRecordID() != null &&
                               log.getRecordID().getMonth() >= 1 &&
                               log.getRecordID().getMonth() <= 12)
                .collect(Collectors.toList());

            ObservableList<AttendanceLog> allLogs = FXCollections.observableArrayList(dbLogs);
            attendanceLog = allLogs.filtered(log -> !isFutureDate(log));
        } catch (Exception e) {
            System.err.println("Error loading attendance logs: " + e.getMessage());
            attendanceLog = FXCollections.observableArrayList();
        }
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

    private void setupColumnWidths() {
        // Set fixed widths for ID and full name columns.
        colNo.setPrefWidth(44);
        colNo.setMinWidth(44);
        colNo.setMaxWidth(44);
        colNo.setResizable(false);

        colFullName.setPrefWidth(180);
        colFullName.setMinWidth(180);
        colFullName.setMaxWidth(180);
        colFullName.setResizable(false);

        // Let the monthly attendance column auto-calculate its width.
        monthAttendanceColumn.setPrefWidth(-1);
    }

    private void setupMonthColumns() {
        monthAttendanceColumn.getColumns().clear();

        String selectedMonthYear = monthYearComboBox.getValue();
        if (selectedMonthYear == null) return;

        LocalDate today = LocalDate.now();
        LocalDate startDate = WeeklyAttendanceUtil.getFirstDayOfMonth(selectedMonthYear);
        LocalDate endDate = (startDate.getMonth() == today.getMonth() &&
                             startDate.getYear() == today.getYear())
                ? today
                : startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<WeeklyAttendanceUtil.WeekDates> weeks = WeeklyAttendanceUtil.splitIntoWeeks(startDate, endDate);
        AtomicInteger weekCounter = new AtomicInteger(1);

        weeks.stream()
             .filter(WeeklyAttendanceUtil.WeekDates::hasWorkingDays)
             .forEach(week -> {
                 TableColumn<Student, String> weekColumn = createWeekColumn("Week " + weekCounter.getAndIncrement());
                 monthAttendanceColumn.getColumns().add(weekColumn);

                 Map<DayOfWeek, TableColumn<Student, String>> dayColumns = 
                         AttendanceDateUtil.createDayNameColumns(true, week.getDates());
                 addDayColumns(weekColumn, dayColumns, week.getDates());
             });

        Platform.runLater(this::adjustColumnWidths);
    }

    private void addDayColumns(TableColumn<Student, String> weekColumn,
                               Map<DayOfWeek, TableColumn<Student, String>> dayColumns,
                               List<LocalDate> dates) {
        dayColumns.values().forEach(dayCol -> {
            weekColumn.getColumns().add(dayCol);
            dates.stream()
                 .filter(date -> date.getDayOfWeek() == dayCol.getUserData())
                 .forEach(date -> dayCol.getColumns().add(createDayColumn(date)));
        });
    }

    private void adjustColumnWidths() {
        TableColumnUtil.adjustColumnWidths(attendanceTable, colNo, colFullName, monthAttendanceColumn);
    }

    @Override
    protected void load_listeners() {
        monthYearComboBox.setOnAction(event -> {
            if (!monthYearComboBox.isFocused()) return;
            setupMonthColumns();
            updateRootControllerMonth();
        });

        exportExcel.setOnAction(event -> handleExport("excel"));
        exportCsv.setOnAction(event -> handleExport("csv"));
        exportPdf.setOnAction(event -> handleExport("pdf"));

        attendanceTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> updateStudentCountLabels());

        attendanceTable.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() > 0) {
                Platform.runLater(this::adjustColumnWidths);
            }
        });
    }

    private void updateRootControllerMonth() {
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
    }

    private void handleExport(String type) {
        try {
            String selectedMonthYear = monthYearComboBox.getValue();
            if (selectedMonthYear == null) return;

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

            AttendanceTableExporter exporter = new AttendanceTableExporter(selectedMonth);
            switch (type) {
                case "excel" -> exporter.exportToExcel(attendanceTable, title, outputPath);
                case "pdf" -> exporter.exportToPdf(attendanceTable, title, outputPath);
                case "csv" -> exporter.exportToCsv(attendanceTable, title, outputPath);
            }
            System.out.println("Export completed: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TableColumn<Student, String> createWeekColumn(String weekLabel) {
        TableColumn<Student, String> weekColumn = new TableColumn<>(weekLabel);
        weekColumn.setStyle("-fx-alignment: CENTER;");
        weekColumn.setMinWidth(150);
        return weekColumn;
    }

    private TableColumn<Student, String> createDayColumn(LocalDate date) {
        TableColumn<Student, String> dayColumn = new TableColumn<>(String.valueOf(date.getDayOfMonth()));
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

        dayColumn.setCellFactory(column -> createDayCell(date));
        return dayColumn;
    }

    private TableCell<Student, String> createDayCell(LocalDate date) {
        TableCell<Student, String> cell = new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || date.isAfter(LocalDate.now())) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                }
            }
        };

        if (!date.isAfter(LocalDate.now())) {
            ContextMenu contextMenu = new ContextMenu();

            MenuItem viewAttendanceItem = new MenuItem("View Attendance Log");
            viewAttendanceItem.setOnAction(e -> {
                Student student = cell.getTableRow().getItem();
                if (student != null) {
                    showAttendanceLogDialog(student, date);
                }
            });

            MenuItem editAttendanceItem = new MenuItem("Edit Attendance");
            editAttendanceItem.setOnAction(e -> {
                Student student = cell.getTableRow().getItem();
                if (student != null) {
                    String currentStatus = cell.getText();
                    var comboBox = new javafx.scene.control.ComboBox<String>();
                    comboBox.getItems().addAll(
                            AttendanceUtil.PRESENT_MARK,
                            AttendanceUtil.ABSENT_MARK,
                            AttendanceUtil.HALF_DAY_MARK,
                            AttendanceUtil.EXCUSED_MARK);
                    comboBox.setValue(currentStatus.isEmpty() ? AttendanceUtil.PRESENT_MARK : currentStatus);
                    cell.setGraphic(comboBox);
                    cell.setText(null);
                    comboBox.setOnAction(event -> updateCellValue(cell, student, date, comboBox.getValue()));
                    comboBox.show();
                }
            });

            contextMenu.getItems().addAll(viewAttendanceItem, editAttendanceItem);
            cell.setContextMenu(contextMenu);
        }
        return cell;
    }

    private String generateLogKey(Student student, AttendanceRecord record) {
        return student.getStudentID() + "-" + record.getRecordID();
    }

    private AttendanceLog findExistingLog(Student student, AttendanceRecord record) {
        String key = generateLogKey(student, record);
        if (logCache.containsKey(key)) {
            return logCache.get(key);
        }
        
        AttendanceLog log = AttendanceLogDAO.getAttendanceLogList().stream()
            .filter(l -> l.getRecordID() != null && 
                        l.getStudentID() != null &&
                        l.getRecordID().getRecordID() == record.getRecordID() &&
                        l.getStudentID().getStudentID() == student.getStudentID())
            .findFirst()
            .orElse(null);
            
        if (log != null) {
            logCache.put(key, log);
        }
        return log;
    }

    private int getNextLogId() {
        return AttendanceLogDAO.getAttendanceLogList().stream()
                .mapToInt(AttendanceLog::getLogID)
                .max()
                .orElse(0) + 1;
    }

    private AttendanceLog createNewLog(Student student, AttendanceRecord record) {
        int nextId = getNextLogId();
        return new AttendanceLog(nextId, record, student, 0, 0, 0, 0);
    }

    private AttendanceRecord findOrCreateRecord(LocalDate date) {
        if (recordCache.containsKey(date)) {
            return recordCache.get(date);
        }

        AttendanceRecord record = AttendanceRecordDAO.getRecordList().stream()
            .filter(r -> r.getYear() == date.getYear() && 
                        r.getMonth() == date.getMonthValue() && 
                        r.getDay() == date.getDayOfMonth())
            .findFirst()
            .orElseGet(() -> {
                AttendanceRecord newRecord = createAttendanceRecord(date);
                AttendanceRecordDAO.insert(newRecord);
                return newRecord;
            });

        recordCache.put(date, record);
        return record;
    }

    private void updateCellValue(TableCell<Student, String> cell, Student student, LocalDate date, String newValue) {
        if (newValue == null) return;

        try {
            AttendanceRecord record = findOrCreateRecord(date);
            AttendanceLog existingLog = findExistingLog(student, record);

            if (existingLog != null) {
                updateLogTimes(existingLog, newValue);
                AttendanceLogDAO.update(existingLog);
            } else {
                AttendanceLog newLog = createNewLog(student, record);
                updateLogTimes(newLog, newValue);
                AttendanceLogDAO.insert(newLog);
                logCache.put(generateLogKey(student, record), newLog);
            }

            cell.setGraphic(null);
            cell.setText(newValue);
            
            refreshDisplay();
        } catch (Exception e) {
            System.err.println("Error updating cell value: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getNextRecordId() {
        return AttendanceRecordDAO.getRecordList().stream()
                .mapToInt(AttendanceRecord::getRecordID)
                .max()
                .orElse(0) + 1;
    }

    private AttendanceRecord createAttendanceRecord(LocalDate date) {
        int nextId = getNextRecordId();
        return new AttendanceRecord(nextId, date.getMonthValue(), date.getDayOfMonth(), date.getYear());
    }

    private void showAttendanceLogDialog(Student student, LocalDate date) {
        try {
            List<AttendanceLog> dialogLogs = new ArrayList<>(AttendanceLogDAO.getAttendanceLogList());
            AttendanceLogDialog dialog = new AttendanceLogDialog(student, date, dialogLogs);
            dialog.load();
        } catch (Exception e) {
            System.err.println("Error showing attendance log dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isFutureDate(AttendanceLog log) {
        try {
            if (log == null || log.getRecordID() == null) return true;
            AttendanceRecord record = log.getRecordID();
            int year = record.getYear();
            int month = record.getMonth();
            int day = record.getDay();
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

    // Update the attendance log's time values based on the attendance status.
    private void updateLogTimes(AttendanceLog log, String attendanceValue) {
        int timeValue;
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
                timeValue = AttendanceUtil.TIME_EXCUSED;
                log.setTimeInAM(timeValue);
                log.setTimeOutAM(timeValue);
                log.setTimeInPM(timeValue);
                log.setTimeOutPM(timeValue);
                break;
        }
    }

    // Refresh the attendance logs from the database and update the UI.
    private void refreshAttendanceLogs() {
        try {
            clearCaches();
            List<AttendanceLog> dbLogs = AttendanceLogDAO.getAttendanceLogList().stream()
                .filter(log -> log != null && 
                             log.getRecordID() != null &&
                             log.getRecordID().getMonth() >= 1 &&
                             log.getRecordID().getMonth() <= 12)
                .collect(Collectors.toList());

            attendanceLog = FXCollections.observableArrayList(dbLogs)
                .filtered(log -> !isFutureDate(log));

            // Pre-populate caches
            dbLogs.forEach(log -> {
                if (log.getStudentID() != null && log.getRecordID() != null) {
                    logCache.put(generateLogKey(log.getStudentID(), log.getRecordID()), log);
                }
            });
                
            refreshDisplay();
        } catch (Exception e) {
            System.err.println("Error refreshing attendance logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshDisplay() {
        Platform.runLater(() -> {
            loadAttendanceLogs();
            setupMonthColumns();
            attendanceTable.refresh();
        });
    }

    private void clearCaches() {
        logCache.clear();
        recordCache.clear();
    }

    public void updateYear(String newYear) {
        initializeWithYear(newYear);
    }

    public void initializeWithYear(String year) {
        if (year == null) return;

        String[] yearRange = year.split("-");
        int startYear = Integer.parseInt(yearRange[0]);
        int endYear = Integer.parseInt(yearRange[1]);

        List<Student> students = StudentDAO.getStudentList().stream()
            .filter(student -> student.getYearID() != null &&
                               student.getYearID().getYearStart() == startYear &&
                               student.getYearID().getYearEnd() == endYear)
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
                .createInstance(StudentProfileLoader.class, getClass().getResource(STUDENT_PROFILE_FXML))
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
        if (studentList == null || attendanceTable == null ||
            selectedStudentsLabel == null || totalStudentsLabel == null) return;

        int totalStudents = studentList.size();
        int selectedStudents = attendanceTable.getSelectionModel().getSelectedItems().size();
        selectedStudentsLabel.setText(String.format("Selected: %d", selectedStudents));
        totalStudentsLabel.setText(String.format("Total: %d", totalStudents));
    }

    public void setSelectedMonth(String monthYear) {
        if (monthYearComboBox != null && monthYearComboBox.getItems().contains(monthYear)) {
            monthYearComboBox.setValue(monthYear);
            setupMonthColumns();
        }
    }
}
