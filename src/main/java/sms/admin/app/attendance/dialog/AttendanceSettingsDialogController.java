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

/**
 * Controller for the attendance settings dialog, managing the configuration of
 * start and end days for attendance tracking in a specific month.
 * This class handles the UI elements and logic for selecting valid weekdays as
 * the start and end days for attendance records.
 */
public class AttendanceSettingsDialogController {

    @FXML
    private ComboBox<Integer> startDayCombo; // ComboBox for selecting the start day of the attendance period
    @FXML
    private ComboBox<Integer> endDayCombo; // ComboBox for selecting the end day of the attendance period
    @FXML
    private Button saveButton; // Button to save the selected settings
    @FXML
    private Button cancelButton; // Button to cancel and close the dialog

    private Stage stage; // The stage for this dialog
    private AttendanceSettings settings; // The attendance settings to configure
    private boolean settingsChanged = false; // Flag indicating if settings were modified
    private LocalDate selectedMonth; // The selected month for the attendance settings

    /**
     * Initializes the controller, setting up event handlers for the save and cancel
     * buttons.
     */
    @FXML
    public void initialize() {
        saveButton.setOnAction(e -> handleSave());
        cancelButton.setOnAction(e -> stage.close());
    }

    /**
     * Sets the selected month-year and initializes the day combo boxes accordingly.
     *
     * @param monthYear The month-year string (e.g., "September 2024") to configure
     *                  settings for.
     */
    public void setSelectedMonthYear(String monthYear) {
        if (monthYear != null) {
            selectedMonth = WeeklyAttendanceUtil.getFirstDayOfMonth(monthYear);
            setupDayComboBoxes();
        }
    }

    /**
     * Sets up the start and end day combo boxes with valid weekdays for the
     * selected month.
     */
    private void setupDayComboBoxes() {
        if (selectedMonth == null) {
            return;
        }

        // Populate combo boxes with weekdays only
        List<Integer> allDays = new ArrayList<>();
        for (int day = 1; day <= selectedMonth.lengthOfMonth(); day++) {
            LocalDate date = selectedMonth.withDayOfMonth(day);
            if (!CommonAttendanceUtil.isWeekend(date)) {
                allDays.add(day);
            }
        }

        startDayCombo.setItems(FXCollections.observableArrayList(allDays));
        endDayCombo.setItems(FXCollections.observableArrayList(allDays));

        // Define a converter to format day numbers as strings
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

        // Set default values to the first and last weekdays
        startDayCombo.setValue(getFirstWeekday());
        endDayCombo.setValue(getLastWeekday());
    }

    /**
     * Sets the stage for this dialog.
     *
     * @param stage The stage to set.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Gets the stage for this dialog.
     *
     * @return The current stage.
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Initializes the dialog with the provided settings and current month-year.
     *
     * @param settings         The attendance settings to configure.
     * @param currentMonthYear The current month-year string (e.g., "September
     *                         2024").
     */
    public void setSettings(AttendanceSettings settings, String currentMonthYear) {
        this.settings = settings.copy();
        setSelectedMonthYear(currentMonthYear); // Populates ComboBox items

        // Validate and set start day
        List<Integer> startDays = startDayCombo.getItems();
        int validatedStartDay = startDays.contains(settings.getStartDay())
                ? settings.getStartDay()
                : startDays.get(0); // Default to first weekday

        // Validate and set end day
        List<Integer> endDays = endDayCombo.getItems();
        int validatedEndDay = endDays.contains(settings.getEndDay())
                ? settings.getEndDay()
                : endDays.get(endDays.size() - 1); // Default to last weekday

        startDayCombo.setValue(validatedStartDay);
        endDayCombo.setValue(validatedEndDay);
    }

    /**
     * Gets the first weekday of the selected month.
     *
     * @return The day of the month for the first weekday.
     */
    private int getFirstWeekday() {
        LocalDate date = selectedMonth.withDayOfMonth(1); // Start from day 1
        while (CommonAttendanceUtil.isWeekend(date) && date.getMonthValue() == selectedMonth.getMonthValue()) {
            date = date.plusDays(1);
        }
        return date.getDayOfMonth();
    }

    /**
     * Gets the last weekday of the selected month.
     *
     * @return The day of the month for the last weekday.
     */
    private int getLastWeekday() {
        LocalDate date = selectedMonth.withDayOfMonth(selectedMonth.lengthOfMonth());
        while (CommonAttendanceUtil.isWeekend(date) && date.getDayOfMonth() > 1) {
            date = date.minusDays(1);
        }
        return date.getDayOfMonth();
    }

    /**
     * Handles the save action, updating the settings if valid and closing the
     * dialog.
     */
    private void handleSave() {
        if (startDayCombo.getValue() != null && endDayCombo.getValue() != null) {
            int startDay = startDayCombo.getValue();
            int endDay = endDayCombo.getValue();

            if (endDay >= startDay) {
                // Update settings and mark as changed
                settings.loadForMonth(selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
                settings.setStartDay(startDay);
                settings.setEndDay(endDay);
                settingsChanged = true;
                System.out.println("Settings saved - Start: " + startDay + ", End: " + endDay);
                stage.close();
            }
        }
    }

    /**
     * Gets the current attendance settings.
     *
     * @return The current AttendanceSettings object.
     */
    public AttendanceSettings getSettings() {
        return settings;
    }

    /**
     * Checks if the settings were changed during the dialog interaction.
     *
     * @return true if settings were changed, false otherwise.
     */
    public boolean isSettingsChanged() {
        return settingsChanged;
    }
}