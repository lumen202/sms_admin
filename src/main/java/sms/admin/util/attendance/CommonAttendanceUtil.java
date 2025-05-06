package sms.admin.util.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Utility class providing common functionality for managing and displaying
 * attendance data.
 * This class includes constants for attendance statuses, methods for formatting
 * time, handling holiday dates,
 * setting up table columns, and computing attendance statuses.
 */
public class CommonAttendanceUtil {

    // Constants for display marks
    public static final String PRESENT_MARK = "✓"; // Symbol for present status
    public static final String HALF_DAY_MARK = "½"; // Symbol for half-day status
    public static final String ABSENT_MARK = "✗"; // Symbol for absent status
    public static final String EXCUSED_MARK = "E"; // Symbol for excused status
    public static final String HOLIDAY_MARK = "H"; // Symbol for holiday status

    // Display text for exports
    public static final String PRESENT_TEXT = "Present"; // Text for present status in exports
    public static final String HALF_DAY_TEXT = "Half Day"; // Text for half-day status in exports
    public static final String ABSENT_TEXT = "Absent"; // Text for absent status in exports
    public static final String EXCUSED_TEXT = "Excused"; // Text for excused status in exports
    public static final String HOLIDAY_TEXT = "Holiday"; // Text for holiday status in exports

    // Time constants (in 24-hour format, e.g., 730 = 7:30 AM)
    public static final int TIME_EXCUSED = 3000; // Special value for excused status
    public static final int TIME_ABSENT = 0; // Special value for absent status
    public static final int TIME_HOLIDAY = -2; // Special value for holiday status
    public static final int TIME_IN_AM = 730; // Default AM check-in time (7:30 AM)
    public static final int TIME_OUT_AM = 1130; // Default AM check-out time (11:30 AM)
    public static final int TIME_IN_PM = 1300; // Default PM check-in time (1:00 PM)
    public static final int TIME_OUT_PM = 1630; // Default PM check-out time (4:30 PM)

    // Cache for attendance status to improve performance
    private static final Map<String, String> attendanceStatusCache = new HashMap<>();

    // Set to track holiday dates
    private static final Set<LocalDate> holidayDates = new HashSet<>();

    /**
     * Converts an attendance mark to its corresponding display text for exports.
     *
     * @param mark The attendance mark (e.g., "✓", "½").
     * @return The display text (e.g., "Present", "Half Day").
     */
    public static String getDisplayText(String mark) {
        return switch (mark) {
            case PRESENT_MARK -> PRESENT_TEXT;
            case HALF_DAY_MARK -> HALF_DAY_TEXT;
            case ABSENT_MARK -> ABSENT_TEXT;
            case EXCUSED_MARK -> EXCUSED_TEXT;
            case HOLIDAY_MARK -> HOLIDAY_TEXT;
            default -> ABSENT_TEXT;
        };
    }

    /**
     * Adds a date to the set of holiday dates.
     *
     * @param date The date to mark as a holiday.
     */
    public static void addHolidayDate(LocalDate date) {
        holidayDates.add(date);
    }

    /**
     * Removes a date from the set of holiday dates.
     *
     * @param date The date to unmark as a holiday.
     */
    public static void removeHolidayDate(LocalDate date) {
        holidayDates.remove(date);
    }

    /**
     * Checks if a date is marked as a holiday.
     *
     * @param date The date to check.
     * @return True if the date is a holiday, false otherwise.
     */
    public static boolean isHolidayDate(LocalDate date) {
        return holidayDates.contains(date);
    }

    /**
     * Configures table columns for displaying student ID and full name.
     *
     * @param colNo       The column for student ID.
     * @param colFullName The column for student full name.
     */
    public static void setupTableColumns(
            TableColumn<Student, Integer> colNo,
            TableColumn<Student, String> colFullName) {
        colNo.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        colNo.setStyle("-fx-alignment: CENTER;");

        colFullName.setCellValueFactory(cell -> {
            Student student = cell.getValue();
            String fullName = String.format("%s, %s %s %s",
                    student.getLastName(),
                    student.getFirstName(),
                    student.getMiddleName(),
                    student.getNameExtension() != null ? student.getNameExtension() : "");
            return new SimpleStringProperty(fullName.trim());
        });
        colFullName.setStyle("-fx-alignment: CENTER-LEFT;");
    }

    /**
     * Determines if a date is a weekend day (Saturday or Sunday).
     *
     * @param date The date to check.
     * @return True if the date is a weekend, false otherwise.
     */
    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /**
     * Returns the abbreviated initial for a day of the week.
     *
     * @param day The day of the week.
     * @return The abbreviated day initial (e.g., "M" for Monday).
     */
    public static String getDayInitial(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "M";
            case TUESDAY -> "T";
            case WEDNESDAY -> "W";
            case THURSDAY -> "Th";
            case FRIDAY -> "F";
            case SATURDAY -> "S";
            case SUNDAY -> "Su";
        };
    }

    /**
     * Finds or creates an attendance log for a student on a specific date.
     *
     * @param student           The student associated with the log.
     * @param date              The date of the attendance log.
     * @param attendanceLogs    The list of existing attendance logs.
     * @param attendanceRecords The list of existing attendance records.
     * @return The existing or newly created AttendanceLog.
     */
    public static AttendanceLog findOrCreateAttendanceLog(
            Student student,
            LocalDate date,
            ObservableList<AttendanceLog> attendanceLogs,
            ObservableList<AttendanceRecord> attendanceRecords) {
        AttendanceLog existingLog = attendanceLogs.stream()
                .filter(log -> isMatchingLog(log, student, date))
                .findFirst()
                .orElse(null);

        if (existingLog != null) {
            return existingLog;
        }

        AttendanceRecord record = new AttendanceRecord(
                attendanceRecords.size() + 1,
                date.getMonthValue(),
                date.getDayOfMonth(),
                date.getYear());
        attendanceRecords.add(record);

        return new AttendanceLog(
                attendanceLogs.size() + 1,
                record,
                student,
                0, 0, 0, 0);
    }

    /**
     * Computes the attendance status based on an attendance log.
     *
     * @param log The attendance log to evaluate.
     * @return The attendance status mark (e.g., "✓", "½").
     */
    public static String computeAttendanceStatus(AttendanceLog log) {
        if (log == null) {
            return ABSENT_MARK;
        }
        if (isHoliday(log)) {
            return HOLIDAY_MARK;
        }
        if (isExcused(log)) {
            return EXCUSED_MARK;
        }
        boolean hasAM = hasValidTimeRange(log.getTimeInAM(), log.getTimeOutAM());
        boolean hasPM = hasValidTimeRange(log.getTimeInPM(), log.getTimeOutPM());
        if (hasAM && hasPM) {
            return PRESENT_MARK;
        } else if (hasAM || hasPM) {
            return HALF_DAY_MARK;
        }
        return ABSENT_MARK;
    }

    /**
     * Checks if an attendance log indicates an excused status.
     *
     * @param log The attendance log to check.
     * @return True if the log indicates excused status, false otherwise.
     */
    public static boolean isExcused(AttendanceLog log) {
        return log != null &&
                log.getTimeInAM() == TIME_EXCUSED &&
                log.getTimeOutAM() == TIME_EXCUSED &&
                log.getTimeInPM() == TIME_EXCUSED &&
                log.getTimeOutPM() == TIME_EXCUSED;
    }

    /**
     * Checks if an attendance log indicates a holiday status.
     *
     * @param log The attendance log to check.
     * @return True if the log indicates holiday status, false otherwise.
     */
    public static boolean isHoliday(AttendanceLog log) {
        return log != null &&
                log.getTimeInAM() == TIME_HOLIDAY &&
                log.getTimeOutAM() == TIME_HOLIDAY &&
                log.getTimeInPM() == TIME_HOLIDAY &&
                log.getTimeOutPM() == TIME_HOLIDAY;
    }

    /**
     * Checks if an attendance log indicates an absent status.
     *
     * @param log The attendance log to check.
     * @return True if the log indicates absent status, false otherwise.
     */
    public static boolean isAbsent(AttendanceLog log) {
        return log != null &&
                log.getTimeInAM() == TIME_ABSENT &&
                log.getTimeOutAM() == TIME_ABSENT &&
                log.getTimeInPM() == TIME_ABSENT &&
                log.getTimeOutPM() == TIME_ABSENT;
    }

    /**
     * Checks if an attendance log matches a specific student and date.
     *
     * @param log     The attendance log to check.
     * @param student The student to match.
     * @param date    The date to match.
     * @return True if the log matches the student and date, false otherwise.
     */
    public static boolean isMatchingLog(AttendanceLog log, Student student, LocalDate date) {
        return log != null &&
                log.getStudentID() != null &&
                log.getRecordID() != null &&
                log.getStudentID().getStudentID() == student.getStudentID() &&
                log.getRecordID().getYear() == date.getYear() &&
                log.getRecordID().getMonth() == date.getMonthValue() &&
                log.getRecordID().getDay() == date.getDayOfMonth();
    }

    /**
     * Validates if a time range is valid (non-zero and non-absent).
     *
     * @param timeIn  The check-in time.
     * @param timeOut The check-out time.
     * @return True if the time range is valid, false otherwise.
     */
    private static boolean hasValidTimeRange(int timeIn, int timeOut) {
        return timeIn > 0 && timeIn != TIME_ABSENT &&
                timeOut > 0 && timeOut != TIME_ABSENT;
    }

    /**
     * Clears the attendance status cache.
     */
    public static void clearAttendanceCache() {
        attendanceStatusCache.clear();
    }

    /**
     * Formats a time value to a 12-hour string representation.
     *
     * @param time The time value to format (e.g., 730 for 7:30 AM).
     * @return The formatted time string (e.g., "07:30 AM").
     */
    public static String formatTime(int time) {
        return formatTime12Hour(time);
    }

    /**
     * Converts an integer time value to a 12-hour format string.
     *
     * @param time The time value to format.
     * @return The formatted time string, or a status text for special values.
     */
    private static String formatTime12Hour(int time) {
        if (time == TIME_ABSENT)
            return "--:--";
        if (time == TIME_EXCUSED)
            return "Excused";
        if (time == TIME_HOLIDAY)
            return "Holiday";

        try {
            int hours = time / 100;
            int minutes = time % 100;
            if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
                return "--:--";
            }
            LocalTime localTime = LocalTime.of(hours, minutes);
            return localTime.format(DateTimeFormatter.ofPattern("hh:mm a"));
        } catch (Exception e) {
            return "--:--";
        }
    }

    /**
     * Formats a LocalTime object to a 12-hour format string.
     *
     * @param time The LocalTime to format.
     * @return The formatted time string, or empty if time is null.
     */
    public static String formatTime12Hour(LocalTime time) {
        if (time == null)
            return "";
        return time.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }
}