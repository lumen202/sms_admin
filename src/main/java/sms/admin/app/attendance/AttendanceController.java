package sms.admin.app.attendance;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
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
import sms.admin.app.attendance.dialog.AttendanceLogDialogLoader;
import sms.admin.util.attendance.AttendanceEditUtil;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.attendance.CommonAttendanceUtil;
import sms.admin.util.attendance.TableColumnUtil;
import sms.admin.util.attendance.WeeklyAttendanceUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.exporter.AttendanceTableExporter;

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
    private MenuItem exportExcel, exportCsv, exportPdf;

    private ObservableList<Student> studentList = FXCollections.observableArrayList();
    private static final ObservableList<AttendanceLog> masterAttendanceLogs = FXCollections.observableArrayList();
    private final NavigableMap<LocalDate, Map<Integer, AttendanceLog>> dateToStudentLogs = new TreeMap<>();
    private boolean isMonthChanging = false;
    private String currentYear;

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

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.widthProperty().addListener((o, oldVal, newVal) -> {
                    double stableWidth = rootPane.getWidth();
                    TableColumnUtil.configureBasicColumns(colNo, colFullName, stableWidth);
                    setupMonthColumns();
                    attendanceTable.refresh();
                });
            }
        });
    }

    private void initializeStudentList(String year) {
        int startYear = Integer.parseInt(year.split("-")[0]);
        ObservableList<Student> students = DataManager.getInstance().getCollectionsRegistry().getList("STUDENT");
        studentList.setAll(students.stream()
                .filter(s -> s != null && s.getYearID() != null
                && s.getYearID().getYearStart() == startYear && s.isDeleted() == 0)
                .collect(Collectors.toList()));
        attendanceTable.setItems(studentList);
    }

    private void loadAttendanceLogs() {
        ObservableList<AttendanceLog> allLogs = DataManager.getInstance().getCollectionsRegistry()
                .getList("ATTENDANCE_LOG");
        if (allLogs == null) {
            masterAttendanceLogs.clear();
            dateToStudentLogs.clear();
            attendanceTable.refresh();
            return;
        }

        Map<String, AttendanceLog> uniqueLogs = new HashMap<>();
        allLogs.forEach(log -> {
            String key = log.getStudentID().getStudentID() + "_" + log.getRecordID().getRecordID();
            uniqueLogs.compute(key, (k, existing)
                    -> existing == null || log.getLogID() > existing.getLogID() ? log : existing);
        });

        List<AttendanceLog> validLogs = uniqueLogs.values().stream()
                .filter(this::isValidLog)
                .filter(log -> !isFutureDate(log))
                .collect(Collectors.toList());

        masterAttendanceLogs.setAll(validLogs);
        updateDateToStudentLogs();
        Platform.runLater(() -> attendanceTable.refresh());
    }

    private void updateDateToStudentLogs() {
        dateToStudentLogs.clear();
        masterAttendanceLogs.forEach(log -> {
            LocalDate date = LocalDate.of(log.getRecordID().getYear(), log.getRecordID().getMonth(),
                    log.getRecordID().getDay());
            dateToStudentLogs.computeIfAbsent(date, k -> new HashMap<>())
                    .put(log.getStudentID().getStudentID(), log);
        });
    }

    private boolean isValidLog(AttendanceLog log) {
        return log != null && log.getRecordID() != null && log.getStudentID() != null;
    }

    private boolean isFutureDate(AttendanceLog log) {
        if (log == null || log.getRecordID() == null) {
            return true;
        }
        try {
            LocalDate logDate = LocalDate.of(log.getRecordID().getYear(), log.getRecordID().getMonth(),
                    log.getRecordID().getDay());
            return logDate.isAfter(LocalDate.now());
        } catch (DateTimeException e) {
            System.err.println("Invalid date in log: " + e.getMessage());
            return true;
        }
    }

    @Override
    protected void load_bindings() {
        attendanceTable.prefHeightProperty().bind(rootPane.heightProperty());
        attendanceTable.setPrefWidth(1200);
        attendanceTable.prefWidthProperty().bind(rootPane.widthProperty());
    }

    @Override
    protected void load_listeners() {
        monthYearComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                setupMonthColumns();
            }
        });
        exportExcel.setOnAction(e -> handleExport("excel"));
        exportCsv.setOnAction(e -> handleExport("csv"));
        exportPdf.setOnAction(e -> handleExport("pdf"));
        attendanceTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> updateStudentCountLabels());
    }

    private void setupTable() {
        colNo.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        colFullName.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue() != null
                ? String.format("%s, %s %s",
                        c.getValue().getLastName(),
                        c.getValue().getFirstName(),
                        c.getValue().getMiddleName())
                : ""));
        TableColumnUtil.configureBasicColumns(colNo, colFullName, rootPane.getWidth());

        monthAttendanceColumn.setMinWidth(500);
        monthAttendanceColumn.setPrefWidth(800);

        attendanceTable.setItems(studentList);
        attendanceTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        attendanceTable.setTableMenuButtonVisible(true);

        Platform.runLater(() -> attendanceTable.refresh());
    }

    private void setupMonthColumns() {
        if (isMonthChanging) {
            return;
        }
        isMonthChanging = true;

        monthAttendanceColumn.getColumns().clear();
        String monthYear = monthYearComboBox.getValue();
        if (monthYear == null) {
            isMonthChanging = false;
            return;
        }

        LocalDate start = WeeklyAttendanceUtil.getFirstDayOfMonth(monthYear);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<WeeklyAttendanceUtil.WeekDates> weeks = WeeklyAttendanceUtil.splitIntoWeeks(start, end);

        double availableWidth = attendanceTable.getWidth() - colNo.getWidth() - colFullName.getWidth() - 20;
        int totalDays = weeks.stream().mapToInt(WeeklyAttendanceUtil::calculateWorkingDays).sum();
        double dayWidth = Math.max(30, availableWidth / Math.max(totalDays, 1));

        AtomicInteger weekNum = new AtomicInteger(1);
        List<TableColumn<Student, String>> weekColumns = weeks.stream()
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

    private TableColumn<Student, String> createDayColumn(LocalDate date, double width) {
        TableColumn<Student, String> col = new TableColumn<>(String.valueOf(date.getDayOfMonth()));
        col.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            if (student == null || date.isAfter(LocalDate.now())) {
                return new SimpleStringProperty("-");
            }
            // Bind to masterAttendanceLogs for reactivity
            AttendanceLog log = masterAttendanceLogs.stream()
                    .filter(l -> l.getStudentID().getStudentID() == student.getStudentID()
                    && l.getRecordID().getYear() == date.getYear()
                    && l.getRecordID().getMonth() == date.getMonthValue()
                    && l.getRecordID().getDay() == date.getDayOfMonth())
                    .findFirst()
                    .orElse(null);
            return new SimpleStringProperty(log != null ? AttendanceUtil.getAttendanceStatus(log) : AttendanceUtil.ABSENT_MARK);
        });
        col.setMinWidth(width);
        col.setPrefWidth(width);
        if (AttendanceUtil.isHolidayDate(date)) {
            col.setStyle("-fx-background-color: red; -fx-alignment: CENTER;");
        } else {
            col.setStyle("-fx-alignment: CENTER;");
        }
        col.setCellFactory(c -> createDayCell(date));
        return col;
    }

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
                    setStyle("-fx-alignment: CENTER;");

                    ContextMenu menu = new ContextMenu();
                    MenuItem viewItem = new MenuItem("View Attendance Log");
                    viewItem.setOnAction(e -> showAttendanceLogDialog(getTableRow().getItem(), date));
                    menu.getItems().add(viewItem);

                    if (!AttendanceUtil.HOLIDAY_MARK.equals(item)) {
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

    private void editCell(TableCell<Student, String> cell, LocalDate date) {
        Student student = cell.getTableRow().getItem();
        if (student == null || date.isAfter(LocalDate.now())) {
            return;
        }
        String currentStatus = cell.getText();
        if (AttendanceUtil.HOLIDAY_MARK.equals(currentStatus)) {
            return;
        }

        AttendanceEditUtil.handleAttendanceEdit(cell, student, date, masterAttendanceLogs, updatedLog -> {
            if (updatedLog != null) {
                AttendanceLog existingLog = masterAttendanceLogs.stream()
                        .filter(l -> l.getStudentID().getStudentID() == student.getStudentID()
                        && l.getRecordID().getRecordID() == updatedLog.getRecordID().getRecordID())
                        .findFirst()
                        .orElse(null);
                if (existingLog != null && existingLog != updatedLog) {
                    masterAttendanceLogs.remove(existingLog);
                }
                int index = masterAttendanceLogs.indexOf(updatedLog);
                if (index >= 0) {
                    masterAttendanceLogs.set(index, updatedLog);
                } else {
                    masterAttendanceLogs.add(updatedLog);
                }
                updateDateToStudentLogs();
                // Force refresh of the specific row and column
                Platform.runLater(() -> {
                    attendanceTable.refresh();
                    // Optionally, force column re-evaluation
                    monthAttendanceColumn.setVisible(false);
                    monthAttendanceColumn.setVisible(true);
                });
            }
            cell.setGraphic(null);
        });
    }

    private void markDayAsHoliday(LocalDate date) {
        int maxLogId = masterAttendanceLogs.stream().mapToInt(AttendanceLog::getLogID).max().orElse(0);
        AttendanceRecord record = ensureAttendanceRecordExists(date);

        for (Student student : studentList) {
            AttendanceLog log = masterAttendanceLogs.stream()
                    .filter(l -> l.getStudentID().getStudentID() == student.getStudentID()
                    && l.getRecordID().getRecordID() == record.getRecordID())
                    .findFirst()
                    .orElse(null);
            if (log != null) {
                log.setTimeInAM(AttendanceUtil.TIME_HOLIDAY);
                log.setTimeOutAM(AttendanceUtil.TIME_HOLIDAY);
                log.setTimeInPM(AttendanceUtil.TIME_HOLIDAY);
                log.setTimeOutPM(AttendanceUtil.TIME_HOLIDAY);
                AttendanceLogDAO.update(log);
            } else {
                log = new AttendanceLog(++maxLogId, record, student,
                        AttendanceUtil.TIME_HOLIDAY,
                        AttendanceUtil.TIME_HOLIDAY,
                        AttendanceUtil.TIME_HOLIDAY,
                        AttendanceUtil.TIME_HOLIDAY);
                AttendanceLogDAO.insert(log);
                masterAttendanceLogs.add(log);
            }
        }
        updateDateToStudentLogs();
        AttendanceUtil.addHoliday(date);
        Platform.runLater(() -> {
            attendanceTable.refresh();
            monthAttendanceColumn.setVisible(false);
            monthAttendanceColumn.setVisible(true);
        });
    }

    private void unmarkDayAsHoliday(LocalDate date) {
        List<AttendanceRecord> recordList = AttendanceRecordDAO.getRecordList();
        AttendanceRecord record = recordList.stream()
                .filter(r -> r.getYear() == date.getYear()
                && r.getMonth() == date.getMonthValue()
                && r.getDay() == date.getDayOfMonth())
                .findFirst()
                .orElse(null);

        if (record != null) {
            AttendanceRecordDAO.delete(record);
            masterAttendanceLogs.removeIf(log -> log.getRecordID().getRecordID() == record.getRecordID());
            updateDateToStudentLogs();
        }

        AttendanceUtil.removeHoliday(date);
        Platform.runLater(() -> {
            attendanceTable.refresh();
            monthAttendanceColumn.setVisible(false);
            monthAttendanceColumn.setVisible(true);
        });
    }

    private AttendanceRecord ensureAttendanceRecordExists(LocalDate date) {
        List<AttendanceRecord> recordList = AttendanceRecordDAO.getRecordList();
        AttendanceRecord record = recordList.stream()
                .filter(r -> r.getYear() == date.getYear()
                && r.getMonth() == date.getMonthValue()
                && r.getDay() == date.getDayOfMonth())
                .findFirst()
                .orElse(null);
        if (record == null) {
            int nextId = recordList.stream()
                    .mapToInt(AttendanceRecord::getRecordID)
                    .max()
                    .orElse(0) + 1;
            record = new AttendanceRecord(nextId, date.getMonthValue(), date.getDayOfMonth(), date.getYear());
            AttendanceRecordDAO.insert(record);
        }
        return record;
    }

    private void handleExport(String type) {
        String monthYear = monthYearComboBox.getValue();
        if (monthYear == null) {
            return;
        }
        String[] parts = monthYear.split(" ");
        YearMonth ym = YearMonth.of(Integer.parseInt(parts[1]), Month.valueOf(parts[0].toUpperCase()));
        String fileName = String.format("attendance_%s.%s", monthYear.replace(" ", "_").toLowerCase(),
                type.equals("excel") ? "xlsx" : type);
        String filePath = System.getProperty("user.home") + "/Downloads/" + fileName;
        AttendanceTableExporter exporter = new AttendanceTableExporter(ym);
        try {
            switch (type) {
                case "excel" ->
                    exporter.exportToExcel(attendanceTable, "Attendance Report - " + monthYear, filePath);
                case "csv" ->
                    exporter.exportToCsv(attendanceTable, "Attendance Report - " + monthYear, filePath);
                case "pdf" ->
                    exporter.exportToPdf(attendanceTable, "Attendance Report - " + monthYear, filePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAttendanceLogDialog(Student student, LocalDate date) {
        try {
            AttendanceLogDialogLoader loader = new AttendanceLogDialogLoader(student, date, masterAttendanceLogs);
            loader.load();
        } catch (Exception e) {
            System.err.println("Error showing attendance log dialog: " + e.getMessage());
        }
    }

    private void updateStudentCountLabels() {
        if (studentList != null && attendanceTable != null) {
            selectedStudentsLabel.setText("Selected: " + attendanceTable.getSelectionModel().getSelectedItems().size());
            totalStudentsLabel.setText("Total: " + studentList.size());
        }
    }

    private String getSelectedYearOrDefault() {
        String year = (String) getParameter("selectedYear");
        if (year == null) {
            int currentYear = LocalDate.now().getYear();
            year = (LocalDate.now().getMonthValue() >= 6 ? currentYear : currentYear - 1) + "-"
                    + (LocalDate.now().getMonthValue() >= 6 ? currentYear + 1 : currentYear);
        }
        return year;
    }

    public String getSelectedMonth() {
        return monthYearComboBox != null ? monthYearComboBox.getValue() : null;
    }

    public ObservableList<AttendanceLog> getAttendanceLogs() {
        return masterAttendanceLogs;
    }

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
}
