package sms.admin.app.attendance.model;

import java.time.LocalDate;
import java.util.List;

import dev.finalproject.data.SettingsDAO;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.Settings;
import sms.admin.util.attendance.WeeklyAttendanceUtil;

public class AttendanceSettings {
    private int startDay;
    private int endDay;
    private Settings dbSettings;
    private String monthYear;

    public AttendanceSettings() {
        this.startDay = 1;
        this.endDay = LocalDate.now().lengthOfMonth();
    }

    // Add constructor with parameters
    public AttendanceSettings(int startDay, int endDay) {
        this.startDay = startDay;
        this.endDay = endDay;
    }

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
                // Create new settings using the actual last day of month
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

    public int getStartDay() {
        return startDay;
    }

    public void setStartDay(int startDay) {
        System.out.println("Setting startDay to: " + startDay + " for " + monthYear);
        this.startDay = startDay;
        updateDatabase();
    }

    public int getEndDay() {
        return endDay;
    }

    public void setEndDay(int endDay) {
        System.out.println("Setting endDay to: " + endDay + " for " + monthYear);
        this.endDay = endDay;
        updateDatabase();
    }

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

    public AttendanceSettings copy() {
        AttendanceSettings copy = new AttendanceSettings(this.startDay, this.endDay);
        copy.monthYear = this.monthYear;
        copy.dbSettings = this.dbSettings;
        return copy;
    }

    @Override
    public String toString() {
        return "AttendanceSettings{startDay=" + startDay + ", endDay=" + endDay + "}";
    }
}
