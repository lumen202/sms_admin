package sms.admin.app.attendance.attendancev2;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import sms.admin.App;
import sms.admin.util.attendance.AttendanceEditUtil;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.attendance.CommonAttendanceUtil;
import dev.finalproject.data.AttendanceLogDAO;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.app.attendance.dialog.AttendanceLogDialogLoader;

public class AttendanceV2Controller extends FXController {

    @FXML
    private ComboBox<String> monthYearComboBox;
    @FXML
    private TableView<Student> attendanceTable;
    @FXML
    private BorderPane rootPane;
    @FXML
    private Label statusLabel;
    @FXML
    private GridPane attendanceGrid;
    @FXML
    private ScrollPane scrollPane; // Added field

    private final ObservableList<Student> studentList = FXCollections.observableArrayList();
    private final ObservableList<AttendanceLog> attendanceLogs = FXCollections.observableArrayList();
    private final Map<String, AttendanceLog> logCache = new HashMap<>();
    private final NavigableMap<LocalDate, Map<Integer, AttendanceLog>> dateToStudentLogs = new TreeMap<>();
    private Node selectedCell = null;

    private static final String SELECTED_STYLE = "-fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-padding: 5;";
    private static final String DEFAULT_CELL_STYLE = "-fx-border-color: #dee2e6; -fx-padding: 5;";
    private static final String HEADER_STYLE = "-fx-font-weight: bold; -fx-alignment: CENTER; -fx-padding: 5; -fx-background-color: #f8f9fa; -fx-border-color: #dee2e6;";
    private boolean isMonthChanging = false;

    @Override
    protected void load_fields() {
        rootPane.getProperties().put("controller", this);
        String selectedYear = getSelectedYearOrDefault();
        initializeStudentList(selectedYear);
        DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, selectedYear);
        String defaultMonth = monthYearComboBox.getItems().isEmpty() ? "" : monthYearComboBox.getItems().get(0);
        String selectedMonth = (String) getParameter("selectedMonth");
        monthYearComboBox.setValue(selectedMonth != null ? selectedMonth : defaultMonth);
        initializeComponents();
        loadAttendanceLogs();
    }

    @Override
    protected void load_bindings() {
        if (attendanceTable != null) {
            attendanceTable.prefHeightProperty().bind(rootPane.heightProperty());
            attendanceTable.prefWidthProperty().bind(rootPane.widthProperty());
        }

        // Simple width binding
        attendanceGrid.prefWidthProperty().bind(scrollPane.widthProperty().subtract(2));

        // Force resize when scene size changes
        rootPane.widthProperty().addListener((obs, old, newVal) -> {
            if (newVal.doubleValue() > 0) {
                Platform.runLater(this::setupGridConstraints);
            }
        });
    }

    @Override
    protected void load_listeners() {
        monthYearComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                refreshAttendanceData();
            }
        });
    }

    private void initializeComponents() {
        attendanceGrid.getChildren().clear();
        attendanceGrid.getColumnConstraints().clear();
        attendanceGrid.getRowConstraints().clear();
        setupGridConstraints();
        displayCurrentMonthDates();
    }

    private void setupGridConstraints() {
        String monthYear = monthYearComboBox.getValue();
        if (monthYear == null) {
            return;
        }

        YearMonth selectedMonth = getSelectedYearMonth();
        int weekdayCount = countWeekdaysInMonth(selectedMonth);

        double width = rootPane.getWidth() - 20; // Account for scroll bar/padding
        if (width > 0) {
            AttendanceV2Utils.resizeGridColumns(attendanceGrid, weekdayCount);
            adjustCellSizes();
        }

        attendanceGrid.heightProperty().addListener((obs, old, newHeight) -> {
            if (newHeight.doubleValue() > 0) {
                Platform.runLater(() -> {
                    AttendanceV2Utils.distributeRowHeights(attendanceGrid, studentList.size());
                    adjustCellSizes();
                });
            }
        });
        AttendanceV2Utils.resizeGridColumns(attendanceGrid, weekdayCount);

        // Use a differently named variable inside the lambda to avoid shadowing
        Platform.runLater(() -> {
            attendanceGrid.requestLayout();
            double gridWidth = attendanceGrid.getWidth();
            if (gridWidth > 0) {
                AttendanceV2Utils.resizeGridColumns(attendanceGrid, weekdayCount);
            }
        });
    }

    private void adjustCellSizes() {
        double cellSize = AttendanceV2Utils.calculateDateColumnWidth(attendanceGrid,
                countWeekdaysInMonth(getSelectedYearMonth()));

        attendanceGrid.getChildren().forEach(node -> {
            if (node instanceof VBox vbox) {
                vbox.setPrefSize(cellSize, cellSize);
                vbox.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            }
        });
    }

    private YearMonth getSelectedYearMonth() {
        String monthYear = monthYearComboBox.getValue();
        if (monthYear == null) {
            return YearMonth.now();
        }
        String[] parts = monthYear.split(" ");
        try {
            Month month = Month.valueOf(parts[0].toUpperCase());
            int year = Integer.parseInt(parts[1]);
            return YearMonth.of(year, month);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    private void displayCurrentMonthDates() {
        attendanceGrid.getChildren().clear();
        attendanceGrid.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");

        // Create headers spanning two rows
        attendanceGrid.add(createHeaderLabel("ID"), 0, 0, 1, 2);
        attendanceGrid.add(createHeaderLabel("Student Name"), 1, 0, 1, 2);

        YearMonth currentMonth = getSelectedYearMonth();
        LocalDate firstDay = currentMonth.atDay(1);
        int colIndex = 2;
        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = firstDay.plusDays(day - 1);
            if (isWeekend(date)) {
                continue;
            }
            Label dayLabel = createHeaderLabel(getShortDayName(date.getDayOfWeek()));
            dayLabel.setStyle(dayLabel.getStyle() + "; -fx-font-size: 13px;");
            if (date.isAfter(LocalDate.now())) {
                dayLabel.setStyle(dayLabel.getStyle() + "; -fx-text-fill: #999999;");
            }
            attendanceGrid.add(dayLabel, colIndex, 0);
            Label dateLabel = createHeaderLabel(String.valueOf(day));
            if (date.isAfter(LocalDate.now())) {
                dateLabel.setStyle(dateLabel.getStyle() + "; -fx-text-fill: #999999;");
            }
            attendanceGrid.add(dateLabel, colIndex, 1);
            colIndex++;
        }
        addStudentsToGrid(colIndex);
    }

    private String getShortDayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY ->
                "M";
            case TUESDAY ->
                "T";
            case WEDNESDAY ->
                "W";
            case THURSDAY ->
                "Th";
            case FRIDAY ->
                "F";
            default ->
                "";
        };
    }

    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setStyle(HEADER_STYLE);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        return label;
    }

    private Label createCellLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-padding: 5; -fx-border-color: #dee2e6; -fx-alignment: CENTER;");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        return label;
    }

    private void addStudentsToGrid(int maxColumns) {
        YearMonth selectedMonth = getSelectedYearMonth();
        LocalDate firstDay = selectedMonth.atDay(1);
        for (int row = 0; row < studentList.size(); row++) {
            Student student = studentList.get(row);
            int gridRow = row + 2; // Account for two header rows
            attendanceGrid.add(createCellLabel(String.format("%04d", student.getStudentID())), 0, gridRow);
            String fullName = String.format("%s, %s %s", student.getLastName(), student.getFirstName(), student.getMiddleName());
            Label nameLabel = createCellLabel(fullName);
            nameLabel.setStyle(nameLabel.getStyle() + "; -fx-alignment: CENTER-LEFT;");
            attendanceGrid.add(nameLabel, 1, gridRow);
            int col = 2;
            for (int day = 1; day <= selectedMonth.lengthOfMonth(); day++) {
                LocalDate date = firstDay.plusDays(day - 1);
                if (isWeekend(date)) {
                    continue;
                }
                attendanceGrid.add(createAttendanceCell(student, date), col++, gridRow);
            }
        }
    }

    private VBox createAttendanceCell(Student student, LocalDate date) {
        VBox cell = new VBox();
        cell.setAlignment(Pos.CENTER);
        cell.setStyle(DEFAULT_CELL_STYLE);
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setMaxHeight(Double.MAX_VALUE);
        if (date.isAfter(LocalDate.now())) {
            Label futureLabel = new Label("-");
            futureLabel.setStyle("-fx-text-fill: #999999;");
            cell.getChildren().add(futureLabel);
            return cell;
        }
        Map<Integer, AttendanceLog> logsForDate = dateToStudentLogs.get(date);
        AttendanceLog log = logsForDate != null ? logsForDate.get(student.getStudentID()) : null;
        String status = CommonAttendanceUtil.computeAttendanceStatus(log);
        Label statusLabel = new Label(status);
        statusLabel.setStyle("-fx-text-fill: #6c757d;");
        cell.getChildren().add(statusLabel);
        // Create context menu for viewing/editing logs
        ContextMenu contextMenu = new ContextMenu();
        MenuItem viewItem = new MenuItem("View Attendance Log");
        MenuItem editItem = new MenuItem("Edit Attendance");
        viewItem.setOnAction(e -> {
            selectedCell = cell;
            handleViewLogs();
        });
        editItem.setOnAction(e -> {
            selectedCell = cell;
            handleEditLogs();
        });
        contextMenu.getItems().addAll(viewItem, editItem);
        cell.setOnContextMenuRequested(e -> {
            selectedCell = cell;
            contextMenu.show(cell, e.getScreenX(), e.getScreenY());
        });
        cell.setOnMouseClicked(e -> {
            if (!date.isAfter(LocalDate.now())) {
                if (e.getClickCount() == 2) {
                    handleAttendanceEdit(cell, student, date);
                } else {
                    handleCellClick(e);
                }
            }
        });
        return cell;
    }

    private void handleAttendanceEdit(VBox cell, Student student, LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            return;
        }
        ComboBox<String> comboBox = AttendanceEditUtil.createAttendanceComboBox(((Label) cell.getChildren().get(0)).getText());
        cell.getChildren().set(0, comboBox);
        comboBox.setOnAction(e -> {
            String newValue = comboBox.getValue();
            updateAttendanceStatus(student, date, newValue);
            Label updatedLabel = new Label(newValue);
            updatedLabel.setStyle("-fx-text-fill: #6c757d;");
            cell.getChildren().set(0, updatedLabel);
        });
        Platform.runLater(() -> {
            comboBox.requestFocus();
            comboBox.show();
        });
    }

    private void updateAttendanceStatus(Student student, LocalDate date, String status) {
        try {
            Map<Integer, AttendanceLog> logsForDate = dateToStudentLogs.computeIfAbsent(date, k -> new HashMap<>());
            AttendanceLog log = logsForDate.get(student.getStudentID());
            if (status.equals(CommonAttendanceUtil.ABSENT_MARK)) {
                if (log != null) {
                    AttendanceLogDAO.delete(log);
                    attendanceLogs.remove(log);
                    logsForDate.remove(student.getStudentID());
                }
            } else {
                if (log == null) {
                    log = AttendanceUtil.findOrCreateAttendanceLog(student, date, attendanceLogs, FXCollections.observableArrayList());
                    attendanceLogs.add(log);
                }
                updateLogTimes(log, status);
                AttendanceLogDAO.update(log);
                logsForDate.put(student.getStudentID(), log);
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error updating attendance");
        }
    }

    private void updateLogTimes(AttendanceLog log, String status) {
        switch (status) {
            case CommonAttendanceUtil.PRESENT_MARK -> {
                log.setTimeInAM(CommonAttendanceUtil.TIME_IN_AM);
                log.setTimeOutAM(CommonAttendanceUtil.TIME_OUT_AM);
                log.setTimeInPM(CommonAttendanceUtil.TIME_IN_PM);
                log.setTimeOutPM(CommonAttendanceUtil.TIME_OUT_PM);
            }
            case CommonAttendanceUtil.ABSENT_MARK -> {
                log.setTimeInAM(CommonAttendanceUtil.TIME_ABSENT);
                log.setTimeOutAM(CommonAttendanceUtil.TIME_ABSENT);
                log.setTimeInPM(CommonAttendanceUtil.TIME_ABSENT);
                log.setTimeOutPM(CommonAttendanceUtil.TIME_ABSENT);
            }
            case CommonAttendanceUtil.HALF_DAY_MARK -> {
                log.setTimeInAM(CommonAttendanceUtil.TIME_IN_AM);
                log.setTimeOutAM(CommonAttendanceUtil.TIME_OUT_AM);
                log.setTimeInPM(CommonAttendanceUtil.TIME_ABSENT);
                log.setTimeOutPM(CommonAttendanceUtil.TIME_ABSENT);
            }
            case CommonAttendanceUtil.EXCUSED_MARK -> {
                log.setTimeInAM(CommonAttendanceUtil.TIME_EXCUSED);
                log.setTimeOutAM(CommonAttendanceUtil.TIME_EXCUSED);
                log.setTimeInPM(CommonAttendanceUtil.TIME_EXCUSED);
                log.setTimeOutPM(CommonAttendanceUtil.TIME_EXCUSED);
            }
        }
    }

    private void handleCellClick(MouseEvent event) {
        VBox clickedCell = (VBox) event.getSource();
        Integer colIndex = GridPane.getColumnIndex(clickedCell);
        LocalDate cellDate = getDateFromColumnIndex(colIndex);
        if (cellDate == null || cellDate.isAfter(LocalDate.now())) {
            return;
        }
        if (selectedCell != null) {
            selectedCell.setStyle(DEFAULT_CELL_STYLE);
        }
        if (selectedCell != clickedCell) {
            clickedCell.setStyle(SELECTED_STYLE);
            selectedCell = clickedCell;
            Integer rowIndex = GridPane.getRowIndex(clickedCell);
            String studentName = getStudentFromRowIndex(rowIndex);
            statusLabel.setText(String.format("Selected: %s - %s", studentName, cellDate));
        } else {
            selectedCell = null;
            statusLabel.setText("");
        }
    }

    private LocalDate getDateFromColumnIndex(Integer colIndex) {
        if (colIndex == null) {
            return null;
        }
        String monthYear = monthYearComboBox.getValue();
        if (monthYear == null) {
            return null;
        }
        String[] parts = monthYear.split(" ");
        int yearNumber = Integer.parseInt(parts[1]);
        YearMonth selectedMonth = YearMonth.of(yearNumber, Month.valueOf(parts[0].toUpperCase()));
        int targetDateColumn = colIndex - 2; // Adjust for ID and Student Name columns
        int currentWeekday = 0;
        for (int day = 1; day <= selectedMonth.lengthOfMonth(); day++) {
            LocalDate date = selectedMonth.atDay(day);
            if (!isWeekend(date)) {
                if (currentWeekday == targetDateColumn) {
                    return date;
                }
                currentWeekday++;
            }
        }
        return null;
    }

    private String getStudentFromRowIndex(Integer rowIndex) {
        if (rowIndex == null || rowIndex <= 1) {
            return null; // Two header rows
        }
        int index = rowIndex - 2;
        if (index < 0 || index >= studentList.size()) {
            return null;
        }
        Student student = studentList.get(index);
        return String.format("%s, %s %s", student.getLastName(), student.getFirstName(), student.getMiddleName());
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private void refreshAttendanceData() {
        if (isMonthChanging) {
            return;
        }
        isMonthChanging = true;
        try {
            displayCurrentMonthDates();
            loadAttendanceLogs();
        } finally {
            isMonthChanging = false;
        }
    }

    public ObservableList<AttendanceLog> getAttendanceLogs() {
        return attendanceLogs;
    }

    public void setSelectedMonth(String monthYear) {
        if (monthYear == null || monthYearComboBox == null) {
            return;
        }
        Platform.runLater(() -> {
            monthYearComboBox.setValue(monthYear);
            refreshAttendanceData();
        });
    }

    public void initializeWithYear(String year) {
        if (year == null) {
            return;
        }
        isMonthChanging = true;
        try {
            DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, year);
            String defaultMonth = monthYearComboBox.getItems().isEmpty() ? "" : monthYearComboBox.getItems().get(0);
            String selectedMonth = (String) getParameter("selectedMonth");
            monthYearComboBox.setValue(selectedMonth != null ? selectedMonth : defaultMonth);
            refreshAttendanceData();
        } finally {
            isMonthChanging = false;
        }
    }

    private String getSelectedYearOrDefault() {
        String year = (String) getParameter("selectedYear");
        if (year == null) {
            int currentYear = LocalDate.now().getYear();
            year = (LocalDate.now().getMonthValue() >= 6 ? currentYear : currentYear - 1)
                    + "-" + (LocalDate.now().getMonthValue() >= 6 ? currentYear + 1 : currentYear);
        }
        return year;
    }

    private boolean isFutureDate(AttendanceLog log) {
        if (log == null || log.getRecordID() == null) {
            return true;
        }
        try {
            LocalDate logDate = LocalDate.of(log.getRecordID().getYear(),
                    log.getRecordID().getMonth(),
                    log.getRecordID().getDay());
            return logDate.isAfter(LocalDate.now());
        } catch (DateTimeException e) {
            System.err.println("Invalid date in log: " + e.getMessage());
            return true;
        }
    }

    private void loadAttendanceLogs() {
        Set<Integer> studentIds = studentList.stream()
                .map(Student::getStudentID)
                .collect(Collectors.toSet());
        List<AttendanceLog> logs = AttendanceLogDAO.getAttendanceLogList().stream()
                .filter(log -> log != null && log.getStudentID() != null
                && studentIds.contains(log.getStudentID().getStudentID())
                && !isFutureDate(log))
                .collect(Collectors.toList());
        attendanceLogs.setAll(logs);
        dateToStudentLogs.clear();
        for (AttendanceLog log : logs) {
            LocalDate date = LocalDate.of(log.getRecordID().getYear(),
                    log.getRecordID().getMonth(),
                    log.getRecordID().getDay());
            dateToStudentLogs.computeIfAbsent(date, k -> new HashMap<>())
                    .put(log.getStudentID().getStudentID(), log);
        }
    }

    private void initializeStudentList(String year) {
        int startYear = Integer.parseInt(year.split("-")[0]);
        ObservableList<Student> students = App.COLLECTIONS_REGISTRY.getList("STUDENT");
        studentList.setAll(students.stream()
                .filter(s -> s != null && s.getYearID() != null && s.getYearID().getYearStart() == startYear)
                .collect(Collectors.toList()));
    }

    private int countWeekdaysInMonth(YearMonth month) {
        int weekdays = 0;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            if (!isWeekend(month.atDay(day))) {
                weekdays++;
            }
        }
        return weekdays;
    }

    private void handleViewLogs() {
        Student student = getSelectedStudent();
        if (student == null) {
            statusLabel.setText("Please select a student first");
            return;
        }
        try {
            new AttendanceLogDialogLoader(student, getSelectedYearMonth().atDay(1), new ArrayList<>(attendanceLogs)).load();
        } catch (Exception e) {
            statusLabel.setText("Error loading attendance logs");
            e.printStackTrace();
        }
    }

    private void handleEditLogs() {
        Student student = getSelectedStudent();
        if (student == null) {
            statusLabel.setText("Please select a student first");
            return;
        }
        YearMonth month = getSelectedYearMonth();
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();
        List<AttendanceLog> studentLogs = attendanceLogs.stream()
                .filter(log -> log.getStudentID().equals(student) && isDateInRange(log, firstDay, lastDay))
                .collect(Collectors.toList());
        try {
            new AttendanceLogDialogLoader(student, firstDay, studentLogs).load();
            loadAttendanceLogs();
            displayCurrentMonthDates();
        } catch (Exception e) {
            statusLabel.setText("Error editing attendance logs");
            e.printStackTrace();
        }
    }

    private Student getSelectedStudent() {
        Integer rowIndex = selectedCell != null ? GridPane.getRowIndex(selectedCell) : null;
        if (rowIndex == null || rowIndex <= 1) {
            statusLabel.setText("Please select a student first");
            return null;
        }
        int index = rowIndex - 2;
        return index < studentList.size() ? studentList.get(index) : null;
    }

    private boolean isDateInRange(AttendanceLog log, LocalDate start, LocalDate end) {
        LocalDate logDate = LocalDate.of(log.getRecordID().getYear(),
                log.getRecordID().getMonth(),
                log.getRecordID().getDay());
        return !logDate.isBefore(start) && !logDate.isAfter(end);
    }
}
