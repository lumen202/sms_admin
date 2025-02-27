package sms.admin.app.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import sms.admin.app.student.viewstudent.StudentProfileLoader;
import sms.admin.util.DateTimeUtils;

public class AttendanceController extends FXController {
    private static final String WEEK_RANGE_SEPARATOR = " - ";
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd");
    private static final String STUDENT_PROFILE_FXML = "/sms.admin/app/management/viewstudent/STUDENT_PROFILE.fxml";

    @FXML
    private Button studentManagementLabel;
    @FXML
    private Button backButton;
    @FXML
    private ComboBox<String> monthYearComboBox;
    @FXML
    private ComboBox<String> weekComboBox;
    @FXML
    private TableView<Map<String, String>> attendanceTable;
    @FXML
    private TableColumn<Map<String, String>, String> studentColumn;
    @FXML
    private TableColumn<Map<String, String>, String> timeRollColumn;

    private Stage stage;
    private ObservableList<Map<String, String>> attendanceData;

    @Override
    protected void load_bindings() {
        // No bindings needed
    }

    @Override
    protected void load_fields() {
        String selectedYear = getSelectedYearOrDefault();
        DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, selectedYear);
    }

    private String getSelectedYearOrDefault() {
        String selectedYear = (String) getParameter("selectedYear");
        return selectedYear != null ? selectedYear : DateTimeUtils.getCurrentAcademicYear();
    }

    @Override
    protected void load_listeners() {
        // No listeners needed
    }

    @FXML
    public void initialize() {
        load();
        setupTableAndData();
        setupComboBoxListeners();
        updateTableColumns();
    }

    private void setupTableAndData() {
        attendanceData = FXCollections.observableArrayList();
        attendanceTable.setItems(attendanceData);
        attendanceTable.setEditable(true);
        populateWeekComboBox();
    }

    private void setupComboBoxListeners() {
        monthYearComboBox.setOnAction(event -> {
            populateWeekComboBox();
            updateTableColumns();
        });
        weekComboBox.setOnAction(event -> updateTableColumns());
    }

    private void populateWeekComboBox() {
        weekComboBox.getItems().clear();
        String selectedMonthYear = monthYearComboBox.getValue();
        if (selectedMonthYear == null)
            return;

        LocalDate firstDayOfMonth = getFirstDayOfMonth(selectedMonthYear);
        populateWeeks(firstDayOfMonth);
        setDefaultWeek(firstDayOfMonth.getMonthValue(), firstDayOfMonth.getYear());
    }

    private LocalDate getFirstDayOfMonth(String selectedMonthYear) {
        String[] parts = selectedMonthYear.split(" ");
        String monthName = parts[0];
        int yearNumber = Integer.parseInt(parts[1]);

        // Convert month name to month number using Month enum
        Month month = Month.valueOf(monthName.toUpperCase());
        int monthNumber = month.getValue();

        LocalDate firstDay = LocalDate.of(yearNumber, monthNumber, 1);

        // Skip to first weekday
        while (isWeekend(firstDay)) {
            firstDay = firstDay.plusDays(1);
        }
        return firstDay;
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private void populateWeeks(LocalDate firstDayOfMonth) {
        LocalDate currentDay = firstDayOfMonth;
        int currentMonth = firstDayOfMonth.getMonthValue();

        while (currentDay.getMonthValue() == currentMonth) {
            // Find the end of the week (Friday) or end of month
            LocalDate weekEnd = findWeekEndDate(currentDay, currentMonth);
            if (weekEnd != null) {
                addWeekToComboBox(currentDay, weekEnd);
                currentDay = weekEnd.plusDays(1);
                // Skip weekend
                while (currentDay.getMonthValue() == currentMonth && isWeekend(currentDay)) {
                    currentDay = currentDay.plusDays(1);
                }
            } else {
                break;
            }
        }
    }

    private LocalDate findWeekEndDate(LocalDate start, int month) {
        LocalDate current = start;
        while (current.getMonthValue() == month) {
            // If we reach Friday or the last day of the month
            if (current.getDayOfWeek() == DayOfWeek.FRIDAY ||
                    current.plusDays(1).getMonthValue() != month) {
                return current;
            }
            current = current.plusDays(1);
        }
        return null;
    }

    private void addWeekToComboBox(LocalDate weekStart, LocalDate weekEnd) {
        String weekRange = weekStart.format(DAY_FORMATTER) +
                WEEK_RANGE_SEPARATOR +
                weekEnd.format(DAY_FORMATTER);
        weekComboBox.getItems().add(weekRange);
    }

    private void setDefaultWeek(int monthNumber, int yearNumber) {
        if (weekComboBox.getItems().isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();

        // Only set current week if we're in the current month
        if (monthNumber == today.getMonthValue() && yearNumber == today.getYear()) {
            // Find the week that contains today
            for (String weekRange : weekComboBox.getItems()) {
                String[] range = weekRange.split(WEEK_RANGE_SEPARATOR);
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);

                LocalDate weekStart = LocalDate.of(yearNumber, monthNumber, start);
                LocalDate weekEnd = LocalDate.of(yearNumber, monthNumber, end);

                if (!today.isBefore(weekStart) && !today.isAfter(weekEnd)) {
                    weekComboBox.setValue(weekRange);
                    return;
                }
            }
        }

        // If not current month or week not found, select first week
        weekComboBox.setValue(weekComboBox.getItems().get(0));
    }

    private void updateTableColumns() {
        String selectedMonthYear = monthYearComboBox.getValue();
        String selectedWeek = weekComboBox.getValue();
        if (selectedMonthYear == null || selectedWeek == null) {
            return;
        }

        // Parse month and year correctly
        String[] parts = selectedMonthYear.split(" ");
        String monthName = parts[0];
        int yearNumber = Integer.parseInt(parts[1]);

        // Get month number using Month enum
        Month month = Month.valueOf(monthName.toUpperCase());
        int monthNumber = month.getValue();

        String[] weekRange = selectedWeek.split(WEEK_RANGE_SEPARATOR);
        int startDay = Integer.parseInt(weekRange[0]);
        int endDay = Integer.parseInt(weekRange[1]);
        LocalDate weekStart = LocalDate.of(yearNumber, monthNumber, startDay);
        LocalDate weekEnd = LocalDate.of(yearNumber, monthNumber, endDay);

        timeRollColumn.getColumns().clear();
        LocalDate date = weekStart;
        while (!date.isAfter(weekEnd)) {
            TableColumn<Map<String, String>, String> dayColumn = new TableColumn<>(
                    date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getDayOfMonth());
            dayColumn.setCellValueFactory(new PropertyValueFactory<>(date.toString()));
            dayColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            timeRollColumn.getColumns().add(dayColumn);
            date = date.plusDays(1);
        }
    }

    public void updateYear(String newYear) {
        DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, newYear);
        populateWeekComboBox();
        updateTableColumns();
    }

    @FXML
    private void handleBackButton() {
        closeCurrentStage();
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

    private void closeCurrentStage() {
        if (backButton != null && backButton.getScene() != null) {
            stage = (Stage) backButton.getScene().getWindow();
            stage.close();
        }
    }
}
