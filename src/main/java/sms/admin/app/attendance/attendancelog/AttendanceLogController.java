package sms.admin.app.attendance.attendancelog;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import sms.admin.app.attendance.AttendanceLoader;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.mock.DataUtil;
import sms.admin.util.scene.SceneLoaderUtil;

public class AttendanceLogController extends FXController {
    @FXML
    private TableView<AttendanceLog> attendanceLogTable;
    @FXML
    private TableColumn<AttendanceLog, Integer> colNo;
    @FXML
    private TableColumn<AttendanceLog, String> colFullName;
    @FXML
    private TableColumn<AttendanceLog, String> timeInAMColumn;
    @FXML
    private TableColumn<AttendanceLog, String> timeOutAMColumn;
    @FXML
    private TableColumn<AttendanceLog, String> timeInPMColumn;
    @FXML
    private TableColumn<AttendanceLog, String> timeOutPMColumn;
    @FXML
    private Label totalRecordsLabel;
    @FXML
    private StackPane contentPane;
    @FXML
    private ComboBox<String> monthYearComboBox;
    @FXML
    private ComboBox<String> dayComboBox;

    private ObservableList<AttendanceLog> attendanceLogs;

    @Override
    protected void load_fields() {
        String selectedYear = getSelectedYearOrDefault();
        DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, selectedYear);

        // Lazy load data asynchronously
        loadAttendanceDataAsync();

        setupTableColumns();
        setupDateSelectors();
    }

    private void loadAttendanceDataAsync() {
        Task<ObservableList<AttendanceLog>> loadTask = new Task<>() {
            @Override
            protected ObservableList<AttendanceLog> call() {
                return DataUtil.createAttendanceLogList(); // Simulated data load
            }
        };

        loadTask.setOnSucceeded(event -> {
            attendanceLogs = loadTask.getValue();
            filterLogsByCurrentDate(); // Load today's records
        });

        loadTask.setOnFailed(event -> System.err.println("Failed to load attendance data."));
        new Thread(loadTask).start();
    }

    private void setupTableColumns() {
        colNo.setCellValueFactory(
                data -> new SimpleObjectProperty<>(attendanceLogTable.getItems().indexOf(data.getValue()) + 1));
        colNo.setStyle("-fx-alignment: CENTER;");

        colFullName.setCellValueFactory(data -> {
            AttendanceLog log = data.getValue();
            Student student = log.getStudentID();
            if (student == null) return new SimpleStringProperty("N/A");

            return new SimpleStringProperty(String.format("%s, %s %s",
                    student.getLastName(),
                    student.getFirstName(),
                    student.getMiddleName() != null ? student.getMiddleName() : "").trim());
        });

        setupTimeColumnsFactory();

        attendanceLogTable.getColumns().forEach(column -> {
            column.setResizable(false);
            if (column != colFullName) {
                column.setStyle("-fx-alignment: CENTER;");
            }
        });
    }

    private void setupTimeColumnsFactory() {
        timeInAMColumn.setCellValueFactory(data ->
                new SimpleStringProperty(AttendanceUtil.formatTime12Hour(data.getValue().getTimeInAM())));

        timeOutAMColumn.setCellValueFactory(data ->
                new SimpleStringProperty(AttendanceUtil.formatTime12Hour(data.getValue().getTimeOutAM())));

        timeInPMColumn.setCellValueFactory(data ->
                new SimpleStringProperty(AttendanceUtil.formatTime12Hour(data.getValue().getTimeInPM())));

        timeOutPMColumn.setCellValueFactory(data ->
                new SimpleStringProperty(AttendanceUtil.formatTime12Hour(data.getValue().getTimeOutPM())));
    }

    private void updateTotalRecordsLabel() {
        Platform.runLater(() -> totalRecordsLabel.setText("Total Records: " + attendanceLogs.size()));
    }

    public void updateYear(String year) {
        if (year == null) return;

        try {
            int yearValue = Integer.parseInt(year.split("-")[0]);
            ObservableList<AttendanceLog> filteredLogs = FXCollections.observableArrayList(
                    attendanceLogs.filtered(log -> log != null &&
                            log.getRecordID() != null &&
                            log.getRecordID().getYear() == yearValue));

            attendanceLogTable.getItems().setAll(filteredLogs);
            updateTotalRecordsLabel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAttendanceRecord() {
        if (contentPane == null) return;

        String currentYear = (String) getParameter("selectedYear");

        if (contentPane.getChildren().isEmpty()) {
            SceneLoaderUtil.loadSceneWithYear(
                    "/sms/admin/app/attendance/ATTENDANCE.fxml",
                    getClass(),
                    AttendanceLoader.class,
                    currentYear,
                    contentPane
            );
        }
    }

    private void setupDateSelectors() {
        monthYearComboBox.setOnAction(e -> populateDayComboBox());
        dayComboBox.setOnAction(e -> filterLogsBySelectedDate());

        populateDayComboBox();
    }

    private void populateDayComboBox() {
        dayComboBox.getItems().clear();
        String selectedMonthYear = monthYearComboBox.getValue();
        if (selectedMonthYear == null) return;

        String[] parts = selectedMonthYear.split(" ");
        String monthName = parts[0];
        int year = Integer.parseInt(parts[1]);

        Month month = Month.valueOf(monthName.toUpperCase());
        YearMonth yearMonth = YearMonth.of(year, month);

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = LocalDate.of(year, month, day);
            if (!isWeekend(date)) {
                dayComboBox.getItems().add(String.format("%02d", day));
            }
        }

        dayComboBox.setValue(dayComboBox.getItems().get(0));
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private void filterLogsBySelectedDate() {
        String selectedMonthYear = monthYearComboBox.getValue();
        String selectedDay = dayComboBox.getValue();

        if (selectedMonthYear == null || selectedDay == null) return;

        try {
            LocalDate selectedDate = AttendanceUtil.parseDateFromMonthYearDay(
                    selectedMonthYear, selectedDay);

            ObservableList<AttendanceLog> filteredLogs = AttendanceUtil.filterLogsByDate(
                    attendanceLogs,
                    selectedDate.getYear(),
                    selectedDate.getMonth(),
                    selectedDate.getDayOfMonth());

            attendanceLogTable.getItems().setAll(filteredLogs);
            updateTotalRecordsLabel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filterLogsByCurrentDate() {
        LocalDate today = LocalDate.now();

        monthYearComboBox.setValue(today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + today.getYear());

        populateDayComboBox();
        dayComboBox.setValue(String.format("%02d", today.getDayOfMonth()));

        filterLogsBySelectedDate();
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

    @Override
    protected void load_bindings() {
    }

    @Override
    protected void load_listeners() {
    }
}
