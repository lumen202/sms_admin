package sms.admin.app.attendance;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
import dev.sol.core.application.FXController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import sms.admin.app.attendance.dialog.AttendanceLogDialogLoader;
import sms.admin.util.attendance.*;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.exporter.AttendanceTableExporter;
import sms.admin.App;

public class AttendanceController extends FXController {

    @FXML private ComboBox<String> monthYearComboBox;
    @FXML private TableView<Student> attendanceTable;
    @FXML private TableColumn<Student, Integer> colNo;
    @FXML private TableColumn<Student, String> colFullName;
    @FXML private TableColumn<Student, String> monthAttendanceColumn;
    @FXML private BorderPane rootPane;
    @FXML private Label selectedStudentsLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private MenuButton exportButton;
    @FXML private MenuItem exportExcel, exportCsv, exportPdf;

    private ObservableList<Student> studentList = FXCollections.observableArrayList();
    private ObservableList<AttendanceLog> attendanceLogs = FXCollections.observableArrayList();
    private final Map<String, AttendanceLog> logCache = new HashMap<>();
    private final Map<LocalDate, AttendanceRecord> recordCache = new HashMap<>();
    private final NavigableMap<LocalDate, Map<Integer, AttendanceLog>> dateToStudentLogs = new TreeMap<>();
    private boolean isMonthChanging = false;

    @Override
    protected void load_fields() {
        rootPane.getProperties().put("controller", this);
        String selectedYear = getSelectedYearOrDefault();
        initializeStudentList(selectedYear);

        if (!studentList.isEmpty()) {
            setupTable();
            loadAttendanceLogs();
            DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, selectedYear);
            String defaultMonth = monthYearComboBox.getItems().get(0);
            String selectedMonth = (String) getParameter("selectedMonth");
            monthYearComboBox.setValue(selectedMonth != null ? selectedMonth : defaultMonth);
            setupMonthColumns();
            updateStudentCountLabels();
        }
    }

    private void initializeStudentList(String year) {
        int startYear = Integer.parseInt(year.split("-")[0]);
        ObservableList<Student> students = App.COLLECTIONS_REGISTRY.getList("STUDENT");
        studentList.setAll(students.stream()
            .filter(s -> s != null && s.getYearID() != null && s.getYearID().getYearStart() == startYear)
            .collect(Collectors.toList()));
    }

    private void loadAttendanceLogs() {
        Set<Integer> studentIds = studentList.stream()
            .map(Student::getStudentID)
            .collect(Collectors.toSet());

        attendanceLogs.setAll(AttendanceLogDAO.getAttendanceLogList().stream()
            .filter(log -> log != null && log.getStudentID() != null && studentIds.contains(log.getStudentID().getStudentID()) && !isFutureDate(log))
            .collect(Collectors.toList()));

        dateToStudentLogs.clear();
        for (AttendanceLog log : attendanceLogs) {
            LocalDate date = LocalDate.of(log.getRecordID().getYear(), log.getRecordID().getMonth(), log.getRecordID().getDay());
            dateToStudentLogs.computeIfAbsent(date, k -> new HashMap<>()).put(log.getStudentID().getStudentID(), log);
        }
    }

    private boolean isFutureDate(AttendanceLog log) {
        if (log == null || log.getRecordID() == null) return true;
        try {
            LocalDate logDate = LocalDate.of(log.getRecordID().getYear(), log.getRecordID().getMonth(), log.getRecordID().getDay());
            return logDate.isAfter(LocalDate.now());
        } catch (DateTimeException e) {
            System.err.println("Invalid date in log: " + e.getMessage());
            return true;
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
            if (newVal != null && !newVal.equals(oldVal)) setupMonthColumns();
        });
        exportExcel.setOnAction(e -> handleExport("excel"));
        exportCsv.setOnAction(e -> handleExport("csv"));
        exportPdf.setOnAction(e -> handleExport("pdf"));
        attendanceTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> updateStudentCountLabels());
        attendanceTable.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) setupMonthColumns();
        });
    }

    private void setupTable() {
        colNo.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        colFullName.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue() != null ? String.format("%s, %s %s", c.getValue().getLastName(), c.getValue().getFirstName(), c.getValue().getMiddleName()) : ""));
        colNo.setPrefWidth(30);
        colNo.setMinWidth(30);
        colNo.setMaxWidth(30);
        colNo.setResizable(false);
        colNo.setStyle("-fx-alignment: CENTER;");

        colFullName.setPrefWidth(130);
        colFullName.setMinWidth(100);
        colFullName.setMaxWidth(150);
        colFullName.setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 10;");

        monthAttendanceColumn.setMinWidth(500);
        monthAttendanceColumn.setPrefWidth(800);

        attendanceTable.setItems(studentList);
        attendanceTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        attendanceTable.setTableMenuButtonVisible(true);
    }

    private void setupMonthColumns() {
        if (isMonthChanging) return;
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
        monthAttendanceColumn.setPrefWidth(availableWidth);

        Platform.runLater(() -> {
            attendanceTable.refresh();
            TableColumnUtil.updateColumnStyles(attendanceTable, 10);
        });
        isMonthChanging = false;
    }

    private TableColumn<Student, String> createWeekColumn(WeeklyAttendanceUtil.WeekDates week, int weekNum, double dayWidth) {
        TableColumn<Student, String> weekColumn = new TableColumn<>("Week " + weekNum);
        weekColumn.setStyle("-fx-alignment: CENTER;");

        Map<DayOfWeek, List<LocalDate>> datesByDay = week.getDates().stream()
            .filter(d -> !CommonAttendanceUtil.isWeekend(d))
            .collect(Collectors.groupingBy(LocalDate::getDayOfWeek, TreeMap::new, Collectors.toList()));

        datesByDay.forEach((day, dates) -> {
            TableColumn<Student, String> dayColumn = new TableColumn<>(day.toString().substring(0, 1));
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
            if (student != null && !date.isAfter(LocalDate.now())) {
                Map<Integer, AttendanceLog> logsForDate = dateToStudentLogs.get(date);
                if (logsForDate != null) {
                    AttendanceLog log = logsForDate.get(student.getStudentID());
                    if (log != null) return new SimpleStringProperty(AttendanceUtil.getAttendanceStatus(log));
                }
                return new SimpleStringProperty(AttendanceUtil.ABSENT_MARK);
            }
            return new SimpleStringProperty("-");
        });
        col.setMinWidth(width);
        col.setPrefWidth(width);
        col.setStyle("-fx-alignment: CENTER;");
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
                } else if (date.isAfter(LocalDate.now())) {
                    setText("-");
                    setStyle("-fx-text-fill: #999999; -fx-alignment: CENTER;");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        };

        if (!date.isAfter(LocalDate.now())) {
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    editCell(cell, date);
                }
            });

            ContextMenu menu = new ContextMenu();
            MenuItem viewItem = new MenuItem("View Attendance Log");
            viewItem.setOnAction(e -> showAttendanceLogDialog(cell.getTableRow().getItem(), date));
            MenuItem editItem = new MenuItem("Edit Attendance");
            editItem.setOnAction(e -> editCell(cell, date));
            menu.getItems().addAll(viewItem, editItem);
            cell.setContextMenu(menu);
        }
        return cell;
    }

    private void editCell(TableCell<Student, String> cell, LocalDate date) {
        Student student = cell.getTableRow().getItem();
        if (student == null || date.isAfter(LocalDate.now())) return;

        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().setAll(AttendanceUtil.PRESENT_MARK, AttendanceUtil.ABSENT_MARK, AttendanceUtil.HALF_DAY_MARK, AttendanceUtil.EXCUSED_MARK);
        String currentStatus = cell.getText();
        combo.setValue(currentStatus.isEmpty() ? AttendanceUtil.PRESENT_MARK : currentStatus);

        cell.setGraphic(combo);
        cell.setText(null);
        combo.show();

        combo.setOnAction(e -> {
            String newValue = combo.getValue();
            updateCellValue(cell, student, date, newValue);
            cell.setGraphic(null);
            cell.setText(newValue);
        });
    }

    private void updateCellValue(TableCell<Student, String> cell, Student student, LocalDate date, String value) {
        if (value == null) return;

        try {
            Map<Integer, AttendanceLog> logs = dateToStudentLogs.computeIfAbsent(date, k -> new HashMap<>());
            AttendanceLog existingLog = logs.get(student.getStudentID());

            if (value.equals(AttendanceUtil.ABSENT_MARK)) {
                if (existingLog != null) {
                    AttendanceLogDAO.delete(existingLog);
                    attendanceLogs.remove(existingLog);
                    logs.remove(student.getStudentID());
                    logCache.remove(student.getStudentID() + "-" + existingLog.getRecordID().getRecordID());
                }
            } else {
                AttendanceLog logToUpdate;
                if (existingLog != null) {
                    logToUpdate = existingLog;
                    updateLogTimes(logToUpdate, value);
                    AttendanceLogDAO.update(logToUpdate);
                } else {
                    AttendanceRecord record = getOrCreateRecord(date);
                    int nextLogId = getNextLogId();
                    logToUpdate = new AttendanceLog(nextLogId, record, student, 0, 0, 0, 0);
                    updateLogTimes(logToUpdate, value);
                    AttendanceLogDAO.insert(logToUpdate);
                    attendanceLogs.add(logToUpdate);
                }
                logCache.put(student.getStudentID() + "-" + logToUpdate.getRecordID().getRecordID(), logToUpdate);
                logs.put(student.getStudentID(), logToUpdate);
            }

            Platform.runLater(() -> {
                cell.setText(value);
                attendanceTable.refresh();
            });
        } catch (Exception e) {
            System.err.println("Error updating attendance: " + e.getMessage());
            Platform.runLater(() -> cell.setText(AttendanceUtil.ABSENT_MARK));
        }
    }

    private int getNextLogId() {
        return Math.max(
            AttendanceLogDAO.getAttendanceLogList().stream().mapToInt(AttendanceLog::getLogID).max().orElse(0),
            attendanceLogs.stream().mapToInt(AttendanceLog::getLogID).max().orElse(0)
        ) + 1;
    }

    private AttendanceRecord getOrCreateRecord(LocalDate date) {
        return recordCache.computeIfAbsent(date, d -> {
            AttendanceRecord record = AttendanceRecordDAO.getRecordList().stream()
                .filter(r -> r.getYear() == date.getYear() && r.getMonth() == date.getMonthValue() && r.getDay() == date.getDayOfMonth())
                .findFirst()
                .orElse(null);
            if (record == null) {
                int nextId = AttendanceRecordDAO.getRecordList().stream().mapToInt(AttendanceRecord::getRecordID).max().orElse(0) + 1;
                record = new AttendanceRecord(nextId, date.getMonthValue(), date.getDayOfMonth(), date.getYear());
                AttendanceRecordDAO.insert(record);
            }
            return record;
        });
    }

    private void updateLogTimes(AttendanceLog log, String value) {
        switch (value) {
            case AttendanceUtil.PRESENT_MARK -> {
                log.setTimeInAM(AttendanceUtil.TIME_IN_AM);
                log.setTimeOutAM(AttendanceUtil.TIME_OUT_AM);
                log.setTimeInPM(AttendanceUtil.TIME_IN_PM);
                log.setTimeOutPM(AttendanceUtil.TIME_OUT_PM);
            }
            case AttendanceUtil.ABSENT_MARK -> {
                log.setTimeInAM(AttendanceUtil.TIME_ABSENT);
                log.setTimeOutAM(AttendanceUtil.TIME_ABSENT);
                log.setTimeInPM(AttendanceUtil.TIME_ABSENT);
                log.setTimeOutPM(AttendanceUtil.TIME_ABSENT);
            }
            case AttendanceUtil.HALF_DAY_MARK -> {
                log.setTimeInAM(AttendanceUtil.TIME_IN_AM);
                log.setTimeOutAM(AttendanceUtil.TIME_OUT_AM);
                log.setTimeInPM(AttendanceUtil.TIME_ABSENT);
                log.setTimeOutPM(AttendanceUtil.TIME_ABSENT);
            }
            case AttendanceUtil.EXCUSED_MARK -> {
                int t = AttendanceUtil.TIME_EXCUSED;
                log.setTimeInAM(t);
                log.setTimeOutAM(t);
                log.setTimeInPM(t);
                log.setTimeOutPM(t);
            }
        }
    }

    private void handleExport(String type) {
        String monthYear = monthYearComboBox.getValue();
        if (monthYear == null) return;
        String[] parts = monthYear.split(" ");
        YearMonth ym = YearMonth.of(Integer.parseInt(parts[1]), Month.valueOf(parts[0].toUpperCase()));
        String fileName = String.format("attendance_%s.%s", monthYear.replace(" ", "_").toLowerCase(), type.equals("excel") ? "xlsx" : type);
        String filePath = System.getProperty("user.home") + "/Downloads/" + fileName;
        AttendanceTableExporter exporter = new AttendanceTableExporter(ym);
        try {
            switch (type) {
                case "excel" -> exporter.exportToExcel(attendanceTable, "Attendance Report - " + monthYear, filePath);
                case "csv" -> exporter.exportToCsv(attendanceTable, "Attendance Report - " + monthYear, filePath);
                case "pdf" -> exporter.exportToPdf(attendanceTable, "Attendance Report - " + monthYear, filePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAttendanceLogDialog(Student student, LocalDate date) {
        try {
            AttendanceLogDialogLoader loader = new AttendanceLogDialogLoader(student, date, new ArrayList<>(attendanceLogs));
            loader.load();
            // No need to store the controller unless you plan to use it later
            // If you need it, uncomment and ensure proper handling: 
            // AttendanceLogDialogController dialogController = loader.getController();
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
            year = (LocalDate.now().getMonthValue() >= 6 ? currentYear : currentYear - 1) + "-" + 
                   (LocalDate.now().getMonthValue() >= 6 ? currentYear + 1 : currentYear);
        }
        return year;
    }

    public String getSelectedMonth() {
        return monthYearComboBox != null ? monthYearComboBox.getValue() : null;
    }

    public ObservableList<AttendanceLog> getAttendanceLogs() {
        return attendanceLogs;
    }

    public void initializeWithYear(String year) {
        if (year == null) return;
        isMonthChanging = true;
        try {
            DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, year);
            String defaultMonth = monthYearComboBox.getItems().get(0);
            String selectedMonth = (String) getParameter("selectedMonth");
            monthYearComboBox.setValue(selectedMonth != null ? selectedMonth : defaultMonth);
            setupMonthColumns();
        } finally {
            isMonthChanging = false;
            updateStudentCountLabels();
        }
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