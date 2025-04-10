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

        // Get only weekdays of the month
        List<Integer> allDays = new ArrayList<>();
        for (int day = 1; day <= selectedMonth.lengthOfMonth(); day++) {
            LocalDate date = selectedMonth.withDayOfMonth(day);
            if (!CommonAttendanceUtil.isWeekend(date)) {
                allDays.add(day);
            }
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
        setSelectedMonthYear(currentMonthYear); // Populates ComboBox items

        // Ensure startDay exists in ComboBox items
        List<Integer> startDays = startDayCombo.getItems();
        int validatedStartDay = startDays.contains(settings.getStartDay())
                ? settings.getStartDay()
                : startDays.get(0); // Default to first weekday

        // Ensure endDay exists in ComboBox items
        List<Integer> endDays = endDayCombo.getItems();
        int validatedEndDay = endDays.contains(settings.getEndDay())
                ? settings.getEndDay()
                : endDays.get(endDays.size() - 1); // Default to last weekday

        startDayCombo.setValue(validatedStartDay);
        endDayCombo.setValue(validatedEndDay);
    }

    private int getFirstWeekday() {
        LocalDate date = selectedMonth.withDayOfMonth(1); // Start from day 1
        while (CommonAttendanceUtil.isWeekend(date) && date.getMonthValue() == selectedMonth.getMonthValue()) {
            date = date.plusDays(1);
        }
        return date.getDayOfMonth();
    }

    private int getLastWeekday() {
        LocalDate date = selectedMonth.withDayOfMonth(selectedMonth.lengthOfMonth());
        while (CommonAttendanceUtil.isWeekend(date) && date.getDayOfMonth() > 1) {
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
