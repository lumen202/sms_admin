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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.BorderPane;
import sms.admin.app.RootController;
import sms.admin.app.attendance.dialog.AttendanceLogDialog;
import sms.admin.app.student.viewstudent.StudentProfileLoader;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.attendance.AttendanceDateUtil;
import sms.admin.util.attendance.WeeklyAttendanceUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.exporter.AttendanceTableExporter;

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
    private Label selectedStudentsLabel;
    @FXML
    private Label totalStudentsLabel;
    @FXML
    private MenuButton exportButton;
    @FXML
    private MenuItem exportExcel;
    @FXML
    private MenuItem exportCsv;
    @FXML
    private MenuItem exportPdf;

    private ObservableList<Student> studentList = FXCollections.observableArrayList();
    private ObservableList<AttendanceLog> attendanceLog = FXCollections.observableArrayList();
    private Map<String, AttendanceLog> logCache = new HashMap<>();
    private Map<LocalDate, AttendanceRecord> recordCache = new HashMap<>();
    private Map<LocalDate, Map<Integer, AttendanceLog>> dateToStudentLogs = new HashMap<>();

    private boolean isMonthChanging = false;

    // Public static observable list for sharing between controllers
    public static final ObservableList<AttendanceLog> CURRENT_LOGS = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        load(); // Calls load_fields(), load_bindings(), and load_listeners() in sequence
    }

    @Override
    protected void load_fields() {
        try {
            rootPane.getProperties().put("controller", this);
            String selectedYear = getSelectedYearOrDefault();

            // Get student list first
            String[] yearRange = selectedYear.split("-");
            int startYear = Integer.parseInt(yearRange[0]);
            int endYear = Integer.parseInt(yearRange[1]);

            List<Student> students = StudentDAO.getStudentList().stream()
                    .filter(student -> student.getYearID() != null &&
                            student.getYearID().getYearStart() == startYear &&
                            student.getYearID().getYearEnd() == endYear)
                    .collect(Collectors.toList());
            studentList.setAll(students);

            // Initialize month combobox
            DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, selectedYear);

            // Setup table before loading month
            setupTable();
            setupColumnWidths();
            loadAttendanceLogs();

            // Initialize with selected month last
            String selectedMonth = (String) getParameter("selectedMonth");
            if (selectedMonth != null && monthYearComboBox.getItems().contains(selectedMonth)) {
                Platform.runLater(() -> {
                    monthYearComboBox.setValue(selectedMonth);
                    setupMonthColumns();
                });
            }

            updateStudentCountLabels();

            // Add width listener for initial layout
            attendanceTable.widthProperty().addListener(new javafx.beans.value.ChangeListener<Number>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends Number> obs, Number oldVal,
                        Number newVal) {
                    if (newVal.doubleValue() > 0) {
                        adjustColumnWidths();
                        attendanceTable.widthProperty().removeListener(this);
                    }
                }
            });

        } catch (Exception e) {
            System.err.println("Error in load_fields: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void load_bindings() {
        attendanceTable.prefHeightProperty().bind(rootPane.heightProperty());
        attendanceTable.prefWidthProperty().bind(rootPane.widthProperty());
    }

    @Override
    protected void load_listeners() {
        monthYearComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                System.out.println("AttendanceController: Month changed to " + newVal);
                setupMonthColumns();
                updateRootController(newVal);
            }
        });

        exportExcel.setOnAction(event -> handleExport("excel"));
        exportCsv.setOnAction(event -> handleExport("csv"));
        exportPdf.setOnAction(event -> handleExport("pdf"));

        attendanceTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, newVal) -> updateStudentCountLabels());

        attendanceTable.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() > 0)
                Platform.runLater(this::adjustColumnWidths);
        });

        attendanceTable.setOnScroll(event -> {
            if (event.isShiftDown()) {
                ScrollBar horizontalScrollBar = findScrollBar(attendanceTable, true);
                if (horizontalScrollBar != null) {
                    double deltaX = event.getDeltaY() / attendanceTable.getWidth();
                    horizontalScrollBar.setValue(horizontalScrollBar.getValue() - deltaX);
                }
                event.consume();
            }
        });
    }

    private void loadAttendanceLogs() {
        try {
            Set<Integer> studentIds = studentList.stream()
                    .map(Student::getStudentID)
                    .collect(Collectors.toSet());

            List<AttendanceLog> dbLogs = AttendanceLogDAO.getAttendanceLogList().stream()
                    .filter(log -> log != null && log.getRecordID() != null &&
                            log.getStudentID() != null && studentIds.contains(log.getStudentID().getStudentID()) &&
                            isValidDate(log.getRecordID()))
                    .collect(Collectors.toList());

            attendanceLog.setAll(dbLogs.stream()
                    .filter(log -> !isFutureDate(log))
                    .collect(Collectors.toList()));

            // Update shared observable list
            CURRENT_LOGS.setAll(attendanceLog);

            System.out.println("Loaded " + attendanceLog.size() + " logs");

            dateToStudentLogs.clear();
            for (AttendanceLog log : attendanceLog) {
                LocalDate date = LocalDate.of(
                        log.getRecordID().getYear(),
                        log.getRecordID().getMonth(),
                        log.getRecordID().getDay());
                dateToStudentLogs.computeIfAbsent(date, k -> new HashMap<>())
                        .put(log.getStudentID().getStudentID(), log);
            }
        } catch (Exception e) {
            System.err.println("Error loading attendance logs: " + e.getMessage());
            attendanceLog.clear();
            dateToStudentLogs.clear();
        }
    }

    private boolean isValidDate(AttendanceRecord record) {
        try {
            LocalDate.of(record.getYear(), record.getMonth(), record.getDay());
            return true;
        } catch (DateTimeException e) {
            System.err.println(String.format(
                    "Invalid date in record %d: year=%d, month=%d, day=%d",
                    record.getRecordID(), record.getYear(), record.getMonth(), record.getDay()));
            return false;
        }
    }

    private void setupTable() {
        colNo.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        colFullName
                .setCellValueFactory(
                        cellData -> new SimpleStringProperty(
                                cellData.getValue() != null
                                        ? String.format("%s, %s %s", cellData.getValue().getLastName(),
                                                cellData.getValue().getFirstName(), cellData.getValue().getMiddleName())
                                        : ""));

        attendanceTable.setItems(studentList);
        attendanceTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        attendanceTable.setTableMenuButtonVisible(true);
        attendanceTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        ScrollBar horizontalScrollBar = findScrollBar(attendanceTable, true);
        if (horizontalScrollBar != null) {
            horizontalScrollBar.setVisible(true);
            horizontalScrollBar.setStyle("-fx-opacity: 1.0;");
        }
    }

    private void setupColumnWidths() {
        colNo.setPrefWidth(44);
        colNo.setMinWidth(44);
        colNo.setMaxWidth(44);
        colNo.setResizable(false);

        colFullName.setPrefWidth(300);
        colFullName.setMinWidth(300);
        colFullName.setMaxWidth(400);
        colFullName.setResizable(true);

        monthAttendanceColumn.setPrefWidth(-1);
        monthAttendanceColumn.setMinWidth(400);
    }

    private void setupMonthColumns() {
        if (isMonthChanging)
            return;
        try {
            isMonthChanging = true;
            Platform.runLater(() -> {
                try {
                    monthAttendanceColumn.getColumns().clear();
                    WeeklyAttendanceUtil.clearCaches();

                    String selectedMonthYear = monthYearComboBox.getValue();
                    if (selectedMonthYear == null)
                        return;

                    LocalDate startDate = WeeklyAttendanceUtil.getFirstDayOfMonth(selectedMonthYear);
                    if (startDate == null)
                        return;

                    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                    List<WeeklyAttendanceUtil.WeekDates> weeks = WeeklyAttendanceUtil.splitIntoWeeks(startDate,
                            endDate);
                    if (weeks.isEmpty())
                        return;

                    double availableWidth = attendanceTable.getWidth() - colNo.getWidth() - colFullName.getWidth();
                    int totalDays = weeks.stream().mapToInt(WeeklyAttendanceUtil::calculateWorkingDays).sum();
                    double dayWidth = Math.max(52, availableWidth / Math.max(totalDays, 1));
                    double totalWidth = Math.max(availableWidth, dayWidth * totalDays);

                    AtomicInteger weekCounter = new AtomicInteger(1);
                    List<TableColumn<Student, ?>> weekColumns = weeks.stream()
                            .filter(WeeklyAttendanceUtil.WeekDates::hasWorkingDays)
                            .map(week -> {
                                TableColumn<Student, String> weekColumn = createWeekColumn(
                                        "Week " + weekCounter.getAndIncrement());
                                int workingDaysInWeek = WeeklyAttendanceUtil.calculateWorkingDays(week);
                                double weekWidth = WeeklyAttendanceUtil.calculateWeekWidth(workingDaysInWeek,
                                        totalWidth, totalDays);
                                configureWeekColumn(weekColumn, weekWidth, week, workingDaysInWeek);
                                return weekColumn;
                            })
                            .collect(Collectors.toList());

                    monthAttendanceColumn.getColumns().addAll(weekColumns);
                    monthAttendanceColumn.setMinWidth(totalWidth);

                    attendanceTable.refresh();
                } finally {
                    isMonthChanging = false;
                }
            });
        } catch (Exception e) {
            isMonthChanging = false;
            e.printStackTrace();
        }
    }

    private void configureWeekColumn(TableColumn<Student, String> weekColumn, double weekWidth,
            WeeklyAttendanceUtil.WeekDates week, int workingDaysInWeek) {
        final double MIN_WIDTH_FACTOR = 0.8;
        weekColumn.setPrefWidth(weekWidth);
        weekColumn.setMinWidth(weekWidth * MIN_WIDTH_FACTOR);

        Map<DayOfWeek, TableColumn<Student, String>> dayColumns = AttendanceDateUtil.createDayNameColumns(true,
                week.getDates());
        double dayWidth = weekWidth / workingDaysInWeek;
        addDayColumns(weekColumn, dayColumns, week.getDates(), dayWidth);
    }

    private void addDayColumns(TableColumn<Student, String> weekColumn,
            Map<DayOfWeek, TableColumn<Student, String>> dayColumns,
            List<LocalDate> dates, double dayWidth) {
        final double MIN_WIDTH_FACTOR = 0.8;
        final double MAX_WIDTH_FACTOR = 1.2;

        dayColumns.forEach((dayOfWeek, dayCol) -> {
            List<TableColumn<Student, String>> daySubColumns = dates.stream()
                    .filter(date -> date.getDayOfWeek() == dayOfWeek)
                    .map(date -> {
                        TableColumn<Student, String> dayColumn = createDayColumn(date);
                        dayColumn.setPrefWidth(dayWidth);
                        dayColumn.setMinWidth(dayWidth * MIN_WIDTH_FACTOR);
                        dayColumn.setMaxWidth(dayWidth * MAX_WIDTH_FACTOR);
                        return dayColumn;
                    })
                    .collect(Collectors.toList());

            if (!daySubColumns.isEmpty()) {
                dayCol.getColumns().addAll(daySubColumns);
                double totalDayWidth = dayWidth * daySubColumns.size();
                dayCol.setPrefWidth(totalDayWidth);
                dayCol.setMinWidth(totalDayWidth * MIN_WIDTH_FACTOR);
                weekColumn.getColumns().add(dayCol);
            }
        });
    }

    private void adjustColumnWidths() {
        if (attendanceTable.getWidth() <= 0 || monthAttendanceColumn.getWidth() <= 0)
            return;

        double availableWidth = (attendanceTable.getWidth() - colNo.getWidth() - colFullName.getWidth()) * 1.01;
        int totalDays = getTotalDays();
        if (totalDays == 0)
            return;

        double widthPerDay = Math.max(52, (availableWidth / totalDays) - 2);
        double totalNeededWidth = widthPerDay * totalDays * 1.01;

        monthAttendanceColumn.setPrefWidth(Math.max(totalNeededWidth, availableWidth));
        monthAttendanceColumn.getColumns().forEach(weekCol -> {
            int daysInWeek = weekCol.getColumns().stream()
                    .mapToInt(dayNameCol -> dayNameCol.getColumns().size())
                    .sum();
            if (daysInWeek > 0) {
                double weekWidth = widthPerDay * daysInWeek;
                weekCol.setPrefWidth(weekWidth);
                weekCol.setMinWidth(weekWidth * 0.8);
                weekCol.getColumns().forEach(dayNameCol -> {
                    double dayWidth = weekWidth / daysInWeek;
                    dayNameCol.setPrefWidth(dayWidth);
                    dayNameCol.setMinWidth(dayWidth * 0.8);
                    dayNameCol.getColumns().forEach(dayCol -> {
                        dayCol.setPrefWidth(dayWidth);
                        dayCol.setMinWidth(dayWidth * 0.8);
                    });
                });
            }
        });
    }

    private TableColumn<Student, String> createWeekColumn(String weekLabel) {
        TableColumn<Student, String> weekColumn = new TableColumn<>(weekLabel);
        weekColumn.setStyle("-fx-alignment: CENTER;");
        return weekColumn;
    }

    private TableColumn<Student, String> createDayColumn(LocalDate date) {
        TableColumn<Student, String> dayColumn = new TableColumn<>(String.valueOf(date.getDayOfMonth()));
        double columnWidth = Math.max(
                AttendanceUtil.PRESENT_MARK.length() * 12 + 20, 52);
        dayColumn.setMinWidth(columnWidth);
        dayColumn.setPrefWidth(columnWidth);
        dayColumn.setMaxWidth(columnWidth * 1.5);
        dayColumn.setResizable(false);
        dayColumn.setStyle("-fx-alignment: CENTER;");

        dayColumn.setCellValueFactory(data -> new SimpleStringProperty(getAttendanceStatus(data.getValue(), date)));
        dayColumn.setCellFactory(column -> createDayCell(date));
        return dayColumn;
    }

    private String getAttendanceStatus(Student student, LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            return "-"; // Return dash for future dates
        }

        Map<Integer, AttendanceLog> studentLogs = dateToStudentLogs.get(date);
        if (studentLogs != null) {
            AttendanceLog log = studentLogs.get(student.getStudentID());
            if (log != null)
                return computeStatusFromLog(log);
        }
        return AttendanceUtil.ABSENT_MARK;
    }

    private String computeStatusFromLog(AttendanceLog log) {
        System.out.println("Computing status for log: " + log.getTimeInAM() + ", " + log.getTimeOutAM() + ", " +
                log.getTimeInPM() + ", " + log.getTimeOutPM());
        if (log.getTimeInAM() == AttendanceUtil.TIME_ABSENT && log.getTimeOutAM() == AttendanceUtil.TIME_ABSENT &&
                log.getTimeInPM() == AttendanceUtil.TIME_ABSENT && log.getTimeOutPM() == AttendanceUtil.TIME_ABSENT) {
            System.out.println("Status: ABSENT");
            return AttendanceUtil.ABSENT_MARK;
        } else if (log.getTimeInAM() == AttendanceUtil.TIME_EXCUSED && log.getTimeOutAM() == AttendanceUtil.TIME_EXCUSED
                &&
                log.getTimeInPM() == AttendanceUtil.TIME_EXCUSED && log.getTimeOutPM() == AttendanceUtil.TIME_EXCUSED) {
            System.out.println("Status: EXCUSED");
            return AttendanceUtil.EXCUSED_MARK;
        } else if (log.getTimeInPM() == AttendanceUtil.TIME_ABSENT
                && log.getTimeOutPM() == AttendanceUtil.TIME_ABSENT) {
            System.out.println("Status: HALF_DAY");
            return AttendanceUtil.HALF_DAY_MARK;
        } else {
            System.out.println("Status: PRESENT (default)");
            return AttendanceUtil.PRESENT_MARK;
        }
    }

    private TableCell<Student, String> createDayCell(LocalDate date) {
        TableCell<Student, String> cell = new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (date.isAfter(LocalDate.now())) {
                    setText("-");
                    setStyle("-fx-text-fill: #999999; -fx-alignment: CENTER;");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        };

        // Double-click to edit attendance
        cell.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !date.isAfter(LocalDate.now())) {
                Student student = cell.getTableRow().getItem();
                if (student != null) {
                    String currentStatus = cell.getText();
                    var comboBox = new javafx.scene.control.ComboBox<String>();
                    comboBox.getItems().addAll(
                            AttendanceUtil.PRESENT_MARK, // e.g., "P"
                            AttendanceUtil.ABSENT_MARK, // e.g., "A"
                            AttendanceUtil.HALF_DAY_MARK, // e.g., "H"
                            AttendanceUtil.EXCUSED_MARK // e.g., "E"
                    );
                    comboBox.setValue(currentStatus.isEmpty() ? AttendanceUtil.PRESENT_MARK : currentStatus);
                    cell.setGraphic(comboBox);
                    cell.setText(null);
                    comboBox.setOnAction(e -> updateCellValue(cell, student, date, comboBox.getValue()));
                    comboBox.show();
                }
            }
        });

        // Retain context menu for right-click
        if (!date.isAfter(LocalDate.now())) {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem viewAttendanceItem = new MenuItem("View Attendance Log");
            viewAttendanceItem.setOnAction(e -> {
                Student student = cell.getTableRow().getItem();
                if (student != null)
                    showAttendanceLogDialog(student, date);
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

    private void updateCellValue(TableCell<Student, String> cell, Student student, LocalDate date, String newValue) {
        if (newValue == null)
            return;

        try {
            Map<Integer, AttendanceLog> studentLogs = dateToStudentLogs.get(date);
            final AttendanceLog existingLog = studentLogs != null ? studentLogs.get(student.getStudentID()) : null;
            final AttendanceLog updatedLog;

            if (existingLog != null) {
                updateLogTimes(existingLog, newValue);
                AttendanceLogDAO.update(existingLog);
                CURRENT_LOGS.removeIf(l -> l.getLogID() == existingLog.getLogID());
                CURRENT_LOGS.add(existingLog);
                updatedLog = existingLog;
                System.out.println("Updated existing log for student: " + student.getStudentID());
            } else {
                AttendanceRecord record = findOrCreateRecord(date);
                AttendanceLog newLog = createNewLog(student, record);
                updateLogTimes(newLog, newValue);
                AttendanceLogDAO.insert(newLog);
                attendanceLog.add(newLog);
                CURRENT_LOGS.add(newLog);
                updatedLog = newLog;
                System.out.println("Created new log for student: " + student.getStudentID());
            }

            logCache.put(generateLogKey(student, updatedLog.getRecordID()), updatedLog);
            dateToStudentLogs.computeIfAbsent(date, k -> new HashMap<>())
                    .put(student.getStudentID(), updatedLog);

            Scene scene = rootPane.getScene();
            if (scene != null && scene.getRoot() != null) {
                Object controller = scene.getRoot().getProperties().get("controller");
                if (controller instanceof RootController rootController) {
                    rootController.refreshCollections();
                }
            }

            Platform.runLater(() -> {
                cell.setGraphic(null);
                cell.setText(newValue);
                attendanceTable.refresh();
            });
        } catch (Exception e) {
            System.err.println("Error updating attendance: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                cell.setGraphic(null);
                cell.setText(AttendanceUtil.ABSENT_MARK);
            });
        }
    }

    public void refreshData() {
        loadAttendanceLogs();
        Platform.runLater(() -> {
            attendanceTable.refresh();
        });
    }

    private AttendanceRecord findOrCreateRecord(LocalDate date) {
        return recordCache.computeIfAbsent(date, d -> AttendanceRecordDAO.getRecordList().stream()
                .filter(r -> r.getYear() == date.getYear() &&
                        r.getMonth() == date.getMonthValue() &&
                        r.getDay() == date.getDayOfMonth())
                .findFirst()
                .orElseGet(() -> {
                    AttendanceRecord newRecord = createAttendanceRecord(date);
                    AttendanceRecordDAO.insert(newRecord);
                    return newRecord;
                }));
    }

    private AttendanceLog findExistingLog(Student student, AttendanceRecord record) {
        String key = generateLogKey(student, record);
        return logCache.computeIfAbsent(key, k -> AttendanceLogDAO.getAttendanceLogList().stream()
                .filter(log -> log != null && log.getRecordID() != null && log.getStudentID() != null &&
                        log.getRecordID().getRecordID() == record.getRecordID() &&
                        log.getStudentID().getStudentID() == student.getStudentID())
                .findFirst()
                .orElse(null));
    }

    private String generateLogKey(Student student, AttendanceRecord record) {
        return student.getStudentID() + "-" + record.getRecordID();
    }

    private AttendanceLog createNewLog(Student student, AttendanceRecord record) {
        int nextId = AttendanceLogDAO.getAttendanceLogList().stream()
                .mapToInt(AttendanceLog::getLogID)
                .max()
                .orElse(0) + 1;
        return new AttendanceLog(nextId, record, student, 0, 0, 0, 0);
    }

    private AttendanceRecord createAttendanceRecord(LocalDate date) {
        int nextId = AttendanceRecordDAO.getRecordList().stream()
                .mapToInt(AttendanceRecord::getRecordID)
                .max()
                .orElse(0) + 1;
        return new AttendanceRecord(nextId, date.getMonthValue(), date.getDayOfMonth(), date.getYear());
    }

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
                System.out.println("Set absent for log: " + log.getStudentID().getStudentID());
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

    private boolean isFutureDate(AttendanceLog log) {
        if (log == null || log.getRecordID() == null)
            return true;
        try {
            LocalDate logDate = LocalDate.of(
                    log.getRecordID().getYear(),
                    log.getRecordID().getMonth(),
                    log.getRecordID().getDay());
            return logDate.isAfter(LocalDate.now());
        } catch (DateTimeException e) {
            System.err.println("Invalid date in log: " + e.getMessage());
            return true;
        }
    }

    private void handleExport(String type) {
        try {
            String selectedMonthYear = monthYearComboBox.getValue();
            if (selectedMonthYear == null)
                return;

            String[] parts = selectedMonthYear.split(" ");
            Month month = Month.valueOf(parts[0].toUpperCase());
            int year = Integer.parseInt(parts[1]);
            YearMonth selectedMonth = YearMonth.of(year, month.getValue());

            String title = "Attendance Report - " + selectedMonthYear;
            String fileName = String.format("attendance_%s.%s",
                    selectedMonthYear.replace(" ", "_").toLowerCase(),
                    type.equals("excel") ? "xlsx" : type.toLowerCase());
            String outputPath = System.getProperty("user.home") + "/Downloads/" + fileName;

            AttendanceTableExporter exporter = new AttendanceTableExporter(selectedMonth);
            switch (type) {
                case "excel":
                    exporter.exportToExcel(attendanceTable, title, outputPath);
                    break;
                case "csv":
                    exporter.exportToCsv(attendanceTable, title, outputPath);
                    break;
                case "pdf":
                    exporter.exportToPdf(attendanceTable, title, outputPath);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ScrollBar findScrollBar(TableView<?> table, boolean horizontal) {
        return (ScrollBar) table.lookupAll(".scroll-bar").stream()
                .filter(node -> node instanceof ScrollBar)
                .map(node -> (ScrollBar) node)
                .filter(bar -> bar.getOrientation() == (horizontal ? javafx.geometry.Orientation.HORIZONTAL
                        : javafx.geometry.Orientation.VERTICAL))
                .findFirst()
                .orElse(null);
    }

    private void showAttendanceLogDialog(Student student, LocalDate date) {
        try {
            List<AttendanceLog> dialogLogs = new ArrayList<>(attendanceLog);
            AttendanceLogDialog dialog = new AttendanceLogDialog(student, date, dialogLogs);
            dialog.load();
        } catch (Exception e) {
            System.err.println("Error showing attendance log dialog: " + e.getMessage());
        }
    }

    public void updateYear(String newYear) {
        initializeWithYear(newYear);
    }

    public void initializeWithYear(String year) {
        if (year == null)
            return;
        try {
            isMonthChanging = true;
            DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, year);

            String prevValue = monthYearComboBox.getValue();
            String selectedMonth = (String) getParameter("selectedMonth");
            String monthToSet = prevValue != null && monthYearComboBox.getItems().contains(prevValue) ? prevValue
                    : selectedMonth != null && monthYearComboBox.getItems().contains(selectedMonth) ? selectedMonth
                            : monthYearComboBox.getItems().stream()
                                    .filter(item -> item.contains(String.valueOf(YearMonth.now().getYear())))
                                    .findFirst()
                                    .orElse(monthYearComboBox.getItems().get(0));

            if (!monthToSet.equals(monthYearComboBox.getValue())) {
                monthYearComboBox.setValue(monthToSet);
                setupMonthColumns();
            }
        } finally {
            isMonthChanging = false;
            updateStudentCountLabels();
        }
    }

    private void updateStudentCountLabels() {
        if (studentList == null || attendanceTable == null)
            return;
        int totalStudents = studentList.size();
        int selectedStudents = attendanceTable.getSelectionModel().getSelectedItems().size();
        selectedStudentsLabel.setText(String.format("Selected: %d", selectedStudents));
        totalStudentsLabel.setText(String.format("Total: %d", totalStudents));
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

    private int getTotalDays() {
        return monthAttendanceColumn.getColumns().stream()
                .mapToInt(weekCol -> weekCol.getColumns().stream()
                        .mapToInt(dayNameCol -> dayNameCol.getColumns().size())
                        .sum())
                .sum();
    }

    private void updateRootController(String monthYear) {
        if (monthYear != null && rootPane.getScene() != null) {
            Scene scene = rootPane.getScene();
            Object controller = scene.getRoot().getProperties().get("controller");
            if (controller instanceof RootController rootController) {
                System.out.println("AttendanceController: Notifying root of month change to " + monthYear);
                rootController.setSelectedMonth(monthYear);
            }
        }
    }

    @FXML
    private void handleViewStudentButton() {
        StudentProfileLoader loader = (StudentProfileLoader) FXLoaderFactory
                .createInstance(StudentProfileLoader.class, getClass().getResource(STUDENT_PROFILE_FXML))
                .initialize();
        loader.load();
    }

    public String getSelectedMonth() {
        if (monthYearComboBox == null)
            return null;
        return monthYearComboBox.getValue();
    }

    public void setSelectedMonth(String monthYear) {
        if (monthYear != null && monthYearComboBox != null &&
                monthYearComboBox.getItems().contains(monthYear)) {
            System.out.println("AttendanceController: Setting month to " + monthYear);
            Platform.runLater(() -> {
                if (!monthYear.equals(monthYearComboBox.getValue())) {
                    monthYearComboBox.setValue(monthYear);
                    setupMonthColumns();
                }
            });
        }
    }
}