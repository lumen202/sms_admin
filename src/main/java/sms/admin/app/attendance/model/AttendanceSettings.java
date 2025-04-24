package sms.admin.app.attendance.model;

import java.time.LocalDate;
import java.util.List;

import dev.finalproject.data.SettingsDAO;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.Settings;
import sms.admin.util.attendance.WeeklyAttendanceUtil;

/**
 * Represents the attendance settings for a specific month, including the start
 * and end days.
 * This class manages loading, updating, and persisting attendance settings to
 * the database.
 */
public class AttendanceSettings {
    private int startDay;
    private int endDay;
    private Settings dbSettings;
    private String monthYear;

    /**
     * Default constructor that initializes the settings with the first and last day
     * of the current month.
     */
    public AttendanceSettings() {
        this.startDay = 1;
        this.endDay = LocalDate.now().lengthOfMonth();
    }

    /**
     * Constructor that initializes the settings with specified start and end days.
     *
     * @param startDay The start day of the attendance period.
     * @param endDay   The end day of the attendance period.
     */
    public AttendanceSettings(int startDay, int endDay) {
        this.startDay = startDay;
        this.endDay = endDay;
    }

    /**
     * Loads the attendance settings for the specified month-year.
     * If settings exist in the database, they are loaded; otherwise, default
     * settings are created.
     *
     * @param monthYear The month-year string (e.g., "September 2024").
     */
    public void loadForMonth(String monthYear) {
        this.monthYear = monthYear;
        try {
            LocalDate firstDay = WeeklyAttendanceUtil.getFirstDayOfMonth(monthYear);
            int lastDayOfMonth = firstDay.lengthOfMonth();

            List<Settings> settingsList = SettingsDAO.getSettingsList();
            dbSettings = settingsList.stream()
                    .filter(s -> monthYear.equals(s.getSettingsID()))
                    .findFirst()
                    .orElse(null);

            if (dbSettings != null) {
                this.startDay = dbSettings.getStart();
                this.endDay = dbSettings.getEnd();
            } else {
                // Create new settings using the actual last day of the month
                dbSettings = new Settings(monthYear, 1, lastDayOfMonth);
                SettingsDAO.insert(dbSettings);
                this.startDay = 1;
                this.endDay = lastDayOfMonth;
            }
            System.out.println("Settings loaded for " + monthYear + " - Start: " + startDay + ", End: " + endDay);
        } catch (Exception e) {
            System.err.println("Error loading settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the start day of the attendance period.
     *
     * @return The start day.
     */
    public int getStartDay() {
        return startDay;
    }

    /**
     * Sets the start day of the attendance period and updates the database.
     *
     * @param startDay The new start day.
     */
    public void setStartDay(int startDay) {
        System.out.println("Setting startDay to: " + startDay + " for " + monthYear);
        this.startDay = startDay;
        updateDatabase();
    }

    /**
     * Gets the end day of the attendance period.
     *
     * @return The end day.
     */
    public int getEndDay() {
        return endDay;
    }

    /**
     * Sets the end day of the attendance period and updates the database.
     *
     * @param endDay The new end day.
     */
    public void setEndDay(int endDay) {
        System.out.println("Setting endDay to: " + endDay + " for " + monthYear);
        this.endDay = endDay;
        updateDatabase();
    }

    /**
     * Updates the database with the current settings.
     * If settings do not exist, they are inserted; otherwise, they are updated.
     */
    private void updateDatabase() {
        try {
            if (dbSettings == null) {
                dbSettings = new Settings(monthYear, startDay, endDay);
                SettingsDAO.insert(dbSettings);
            } else {
                dbSettings.setStart(startDay);
                dbSettings.setEnd(endDay);
                SettingsDAO.update(dbSettings);
            }
            DataManager.getInstance().refreshData();
        } catch (Exception e) {
            System.err.println("Error updating settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates a copy of the current settings.
     *
     * @return A new AttendanceSettings object with the same values.
     */
    public AttendanceSettings copy() {
        AttendanceSettings copy = new AttendanceSettings(this.startDay, this.endDay);
        copy.monthYear = this.monthYear;
        copy.dbSettings = this.dbSettings;
        return copy;
    }

    /**
     * Returns a string representation of the settings.
     *
     * @return A string in the format "AttendanceSettings{startDay=X, endDay=Y}".
     */
    @Override
    public String toString() {
        return "AttendanceSettings{startDay=" + startDay + ", endDay=" + endDay + "}";
    }
}