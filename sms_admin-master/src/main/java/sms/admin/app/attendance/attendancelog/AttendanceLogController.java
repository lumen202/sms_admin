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
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
        // Initialize date selectors
        String selectedYear = getSelectedYearOrDefault();
        DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, selectedYear);

        // Get data
        // attendanceLogs = App.COLLECTIONS_REGISTRY.getList("ATTENDANCE_LOG");
        attendanceLogs =DataUtil.createAttendanceLogList();

        // Setup UI
        setupTableColumns();
        setupDateSelectors();

        // Initialize with today's records instead of all records
        filterLogsByCurrentDate();
    }

    private void setupTableColumns() {
        // Row numbers
        colNo.setCellValueFactory(
                data -> new SimpleObjectProperty<>(attendanceLogTable.getItems().indexOf(data.getValue()) + 1));
        colNo.setStyle("-fx-alignment: CENTER;");

        // Student name
        colFullName.setCellValueFactory(data -> {
            AttendanceLog log = data.getValue();
            Student student = log.getStudentID();
            if (student == null)
                return new SimpleStringProperty("N/A");

            return new SimpleStringProperty(String.format("%s, %s %s",
                    student.getLastName(),
                    student.getFirstName(),
                    student.getMiddleName() != null ? student.getMiddleName() : "").trim());
        });

        // Time columns
        setupTimeColumnsFactory();

        // Column styling
        attendanceLogTable.getColumns().forEach(column -> {
            column.setResizable(false);
            if (column != colFullName) {
                column.setStyle("-fx-alignment: CENTER;");
            }
        });
    }

    private void setupTimeColumnsFactory() {
        timeInAMColumn.setCellValueFactory(data -> {
            AttendanceLog log = data.getValue();
            return new SimpleStringProperty(AttendanceUtil.formatTime12Hour(log.getTimeInAM()));
        });

        timeOutAMColumn.setCellValueFactory(data -> {
            AttendanceLog log = data.getValue();
            return new SimpleStringProperty(AttendanceUtil.formatTime12Hour(log.getTimeOutAM()));  // Fixed incorrect time method call
        });

        timeInPMColumn.setCellValueFactory(data -> {
            AttendanceLog log = data.getValue();
            return new SimpleStringProperty(AttendanceUtil.formatTime12Hour(log.getTimeInPM()));  // Fixed incorrect time method call
        });

        timeOutPMColumn.setCellValueFactory(data -> {
            AttendanceLog log = data.getValue();
            return new SimpleStringProperty(AttendanceUtil.formatTime12Hour(log.getTimeOutPM()));  // Fixed incorrect time method call
        });
    }

    private void updateTotalRecordsLabel() {
        if (totalRecordsLabel != null) {
            totalRecordsLabel.setText("Total Records: " + attendanceLogs.size());
        }
    }

    public void updateYear(String year) {
        if (year == null) return;

        try {
            int yearValue = Integer.parseInt(year.split("-")[0]);
            ObservableList<AttendanceLog> filteredLogs = FXCollections.observableArrayList(
                    attendanceLogs.filtered(log -> log != null &&
                            log.getRecordID() != null &&
                            log.getRecordID().getYear() == yearValue));

            attendanceLogTable.setItems(filteredLogs);
            updateTotalRecordsLabel();
        } catch (Exception e) {
            // Silently handle error
        }
    }

    @FXML
    private void handleAttendanceRecord() {
        if (contentPane == null) return;
        
        String currentYear = (String) getParameter("selectedYear");
        
        SceneLoaderUtil.loadSceneWithYear(
                "/sms/admin/app/attendance/ATTENDANCE.fxml",
                getClass(),
                AttendanceLoader.class,
                currentYear,
                contentPane);
    }

    private void setupDateSelectors() {
        // Month/Year combo box listener
        monthYearComboBox.setOnAction(e -> populateDayComboBox());

        // Day combo box listener
        dayComboBox.setOnAction(e -> filterLogsBySelectedDate());

        // Initialize days for current month
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

        // Add only weekdays of the month
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = LocalDate.of(year, month, day);
            if (!isWeekend(date)) {
                dayComboBox.getItems().add(String.format("%02d", day));
            }
        }

        // Select current day if it's the current month and a weekday
        LocalDate today = LocalDate.now();
        if (today.getYear() == year && today.getMonth() == month) {
            if (!isWeekend(today)) {
                dayComboBox.setValue(String.format("%02d", today.getDayOfMonth()));
            } else {
                // If today is weekend, select previous Friday or next Monday
                LocalDate nextValidDay = today;
                while (isWeekend(nextValidDay)) {
                    nextValidDay = nextValidDay.plusDays(1);
                }
                if (nextValidDay.getMonth() == month) {
                    dayComboBox.setValue(String.format("%02d", nextValidDay.getDayOfMonth()));
                } else {
                    // If next valid day is in next month, select last weekday of current month
                    LocalDate lastDay = yearMonth.atEndOfMonth();
                    while (isWeekend(lastDay)) {
                        lastDay = lastDay.minusDays(1);
                    }
                    dayComboBox.setValue(String.format("%02d", lastDay.getDayOfMonth()));
                }
            }
        } else {
            // If not current month, select first available day
            dayComboBox.setValue(dayComboBox.getItems().get(0));
        }
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private void filterLogsBySelectedDate() {
        String selectedMonthYear = monthYearComboBox.getValue();
        String selectedDay = dayComboBox.getValue();

        if (selectedMonthYear == null || selectedDay == null)
            return;

        try {
            LocalDate selectedDate = AttendanceUtil.parseDateFromMonthYearDay(
                    selectedMonthYear, selectedDay);

            ObservableList<AttendanceLog> filteredLogs = AttendanceUtil.filterLogsByDate(
                    attendanceLogs,
                    selectedDate.getYear(),
                    selectedDate.getMonth(),
                    selectedDate.getDayOfMonth());

            attendanceLogTable.setItems(filteredLogs);
            updateTotalRecordsLabel();
        } catch (Exception e) {
            // Silently handle error
        }
    }

    private void filterLogsByCurrentDate() {
        LocalDate today = LocalDate.now();

        // Set the month/year combo box to current month
        monthYearComboBox.setValue(today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + today.getYear());

        // Populate and set day combo box
        populateDayComboBox();
        dayComboBox.setValue(String.format("%02d", today.getDayOfMonth()));

        // Filter logs for today
        filterLogsBySelectedDate();
    }

    private String getSelectedYearOrDefault() {
        // Try to get the selected year from parameters
        String selectedYear = (String) getParameter("selectedYear");

        // If no year was passed, use current academic year
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
