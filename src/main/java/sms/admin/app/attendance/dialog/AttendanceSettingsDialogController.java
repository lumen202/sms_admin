package sms.admin.app.attendance.dialog;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import sms.admin.app.attendance.model.AttendanceSettings;
import sms.admin.util.attendance.CommonAttendanceUtil;
import sms.admin.util.attendance.WeeklyAttendanceUtil;

public class AttendanceSettingsDialogController {

    @FXML
    private ComboBox<Integer> startDayCombo;
    @FXML
    private ComboBox<Integer> endDayCombo;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private Stage stage;
    private AttendanceSettings settings;
    private boolean settingsChanged = false;
    private LocalDate selectedMonth;

    @FXML
    public void initialize() {
        saveButton.setOnAction(e -> handleSave());
        cancelButton.setOnAction(e -> stage.close());
    }

    public void setSelectedMonthYear(String monthYear) {
        if (monthYear != null) {
            selectedMonth = WeeklyAttendanceUtil.getFirstDayOfMonth(monthYear);
            setupDayComboBoxes();
        }
    }

    private void setupDayComboBoxes() {
        if (selectedMonth == null) {
            return;
        }

        // Get all days of the month, not just weekdays
        List<Integer> allDays = new ArrayList<>();
        for (int day = 1; day <= selectedMonth.lengthOfMonth(); day++) {
            allDays.add(day);
        }

        startDayCombo.setItems(FXCollections.observableArrayList(allDays));
        endDayCombo.setItems(FXCollections.observableArrayList(allDays));

        StringConverter<Integer> dayConverter = new StringConverter<>() {
            @Override
            public String toString(Integer day) {
                if (day == null) {
                    return "";
                }
                return String.format("%d", day);
            }

            @Override
            public Integer fromString(String string) {
                if (string == null || string.isEmpty()) {
                    return null;
                }
                return Integer.parseInt(string);
            }
        };

        startDayCombo.setConverter(dayConverter);
        endDayCombo.setConverter(dayConverter);

        // Set default values
        startDayCombo.setValue(getFirstWeekday());
        endDayCombo.setValue(getLastWeekday());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }

    public void setSettings(AttendanceSettings settings, String currentMonthYear) {
        this.settings = settings.copy();
        setSelectedMonthYear(currentMonthYear);

        System.out.println("Loading settings - Start: " + settings.getStartDay() + ", End: " + settings.getEndDay());

        // Set values based on saved settings
        startDayCombo.setValue(settings.getStartDay());
        endDayCombo.setValue(settings.getEndDay());
    }

    private int getFirstWeekday() {
        LocalDate date = selectedMonth;
        while (CommonAttendanceUtil.isWeekend(date)) {
            date = date.plusDays(1);
        }
        return date.getDayOfMonth();
    }

    private int getLastWeekday() {
        LocalDate date = selectedMonth.withDayOfMonth(selectedMonth.lengthOfMonth());
        while (CommonAttendanceUtil.isWeekend(date)) {
            date = date.minusDays(1);
        }
        return date.getDayOfMonth();
    }

    private void handleSave() {
        if (startDayCombo.getValue() != null && endDayCombo.getValue() != null) {
            int startDay = startDayCombo.getValue();
            int endDay = endDayCombo.getValue();
            
            if (endDay >= startDay) {
                // Update settings and handle change
                settings.loadForMonth(selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
                settings.setStartDay(startDay);
                settings.setEndDay(endDay);
                settingsChanged = true;
                System.out.println("Settings saved - Start: " + startDay + ", End: " + endDay);
                stage.close();
            }
        }
    }

    public AttendanceSettings getSettings() {
        return settings;
    }

    public boolean isSettingsChanged() {
        return settingsChanged;
    }
}
