package sms.admin.util.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class AttendanceUtil {
    // Constants for display marks
    public static final String PRESENT_MARK = "✓";
    public static final String HALF_DAY_MARK = "½";
    public static final String ABSENT_MARK = "✗";
    public static final String EXCUSED_MARK = "E";
    public static final String HOLIDAY_MARK = "H";

    // Display text for exports
    public static final String PRESENT_TEXT = "Present";
    public static final String HALF_DAY_TEXT = "Half Day";
    public static final String ABSENT_TEXT = "Absent";
    public static final String EXCUSED_TEXT = "Excused";
    public static final String HOLIDAY_TEXT = "Holiday";

    // Time constants
    public static final int TIME_EXCUSED = 3000;
    public static final int TIME_ABSENT = 0;
    public static final int TIME_HOLIDAY = -2;
    public static final int TIME_IN_AM = 730;
    public static final int TIME_OUT_AM = 1130;
    public static final int TIME_IN_PM = 1300;
    public static final int TIME_OUT_PM = 1630;

    // Cache for attendance status
    private static final Map<String, String> attendanceStatusCache = new HashMap<>();

    // Holiday date tracker
    private static final Set<LocalDate> holidayDates = new HashSet<>();

    // Convert marks to readable text
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

    // Add a holiday date
    public static void addHoliday(LocalDate date) {
        holidayDates.add(date);
    }

    // Remove a holiday date (new method)
    public static void removeHoliday(LocalDate date) {
        holidayDates.remove(date);
    }

    // Check if a date is marked as a holiday
    public static boolean isHolidayDate(LocalDate date) {
        return holidayDates.contains(date);
    }

    // Set up table columns for student display
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

    // Check if a date is a weekend
    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    // Find or create an attendance log for a student on a specific date
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

    // Utility methods for AttendanceRecord
    public static LocalDate getDateFromRecord(AttendanceRecord record) {
        return LocalDate.of(record.getYear(), record.getMonth(), record.getDay());
    }

    public static boolean recordMatchesDate(AttendanceRecord record, LocalDate date) {
        if (record == null || date == null)
            return false;
        return record.getYear() == date.getYear() &&
                record.getMonth() == date.getMonthValue() &&
                record.getDay() == date.getDayOfMonth();
    }

    // Get attendance status using cache (for a student on a given date)
    public static String getAttendanceStatus(Student student, LocalDate date,
            ObservableList<AttendanceLog> attendanceLogs) {
        if (student == null || date == null || attendanceLogs == null) {
            return ABSENT_MARK;
        }
        String cacheKey = String.format("%d_%s", student.getStudentID(), date);
        return attendanceStatusCache.computeIfAbsent(cacheKey,
                k -> calculateAttendanceStatus(student, date, attendanceLogs));
    }

    // Calculate attendance status from student, date, and logs
    private static String calculateAttendanceStatus(Student student, LocalDate date,
            ObservableList<AttendanceLog> attendanceLogs) {
        AttendanceLog log = attendanceLogs.stream()
                .filter(l -> isMatchingLog(l, student, date))
                .findFirst()
                .orElse(null);
        return determineStatus(log);
    }

    // Get attendance status directly from an AttendanceLog
    public static String getAttendanceStatus(AttendanceLog log) {
        return determineStatus(log);
    }

    // Centralized logic to determine attendance status from a log.
    // Holiday check is done before other rules.
    private static String determineStatus(AttendanceLog log) {
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
        } else {
            return ABSENT_MARK;
        }
    }

    // Check if a log matches a student and date
    private static boolean isMatchingLog(AttendanceLog log, Student student, LocalDate date) {
        return log != null &&
                log.getStudentID() != null &&
                log.getRecordID() != null &&
                log.getStudentID().getStudentID() == student.getStudentID() &&
                log.getRecordID().getYear() == date.getYear() &&
                log.getRecordID().getMonth() == date.getMonthValue() &&
                log.getRecordID().getDay() == date.getDayOfMonth();
    }

    // Check if a time range is valid
    private static boolean hasValidTimeRange(int timeIn, int timeOut) {
        return timeIn > 0 && timeIn != TIME_ABSENT &&
                timeOut > 0 && timeOut != TIME_ABSENT;
    }

    // Clear the attendance status cache
    public static void clearCache() {
        attendanceStatusCache.clear();
    }

    // Check if a log indicates an excused absence
    public static boolean isExcused(AttendanceLog log) {
        return log.getTimeInAM() == TIME_EXCUSED &&
                log.getTimeOutAM() == TIME_EXCUSED &&
                log.getTimeInPM() == TIME_EXCUSED &&
                log.getTimeOutPM() == TIME_EXCUSED;
    }

    // Check if a log indicates a full absence
    public static boolean isAbsent(AttendanceLog log) {
        return log.getTimeInAM() == TIME_ABSENT &&
                log.getTimeOutAM() == TIME_ABSENT &&
                log.getTimeInPM() == TIME_ABSENT &&
                log.getTimeOutPM() == TIME_ABSENT;
    }

    // Check if a log indicates full day presence
    public static boolean isFullDayPresent(AttendanceLog log) {
        return log.getTimeInAM() > 0 && log.getTimeInAM() != TIME_EXCUSED &&
                log.getTimeOutAM() > 0 && log.getTimeOutAM() != TIME_EXCUSED &&
                log.getTimeInPM() > 0 && log.getTimeInPM() != TIME_EXCUSED &&
                log.getTimeOutPM() > 0 && log.getTimeOutPM() != TIME_EXCUSED;
    }

    // Check if a log has any time records
    public static boolean hasAnyTimeRecord(AttendanceLog log) {
        return log.getTimeInAM() > 0 || log.getTimeOutAM() > 0 ||
                log.getTimeInPM() > 0 || log.getTimeOutPM() > 0;
    }

    // Check if a log indicates a holiday.
    public static boolean isHoliday(AttendanceLog log) {
        return log.getTimeInAM() == TIME_HOLIDAY &&
                log.getTimeOutAM() == TIME_HOLIDAY &&
                log.getTimeInPM() == TIME_HOLIDAY &&
                log.getTimeOutPM() == TIME_HOLIDAY;
    }

    // Filter logs by date
    public static ObservableList<AttendanceLog> filterLogsByDate(
            ObservableList<AttendanceLog> logs,
            int year,
            Month month,
            int day) {
        return FXCollections.observableArrayList(
                logs.filtered(log -> {
                    if (log == null || log.getRecordID() == null)
                        return false;
                    AttendanceRecord record = log.getRecordID();
                    return record.getYear() == year &&
                            record.getMonth() == month.getValue() &&
                            record.getDay() == day;
                }));
    }

    // Format time as a string
    public static String formatTime(int time) {
        return formatTime12Hour(time);
    }

    // Parse a date from month, year, and day strings
    public static LocalDate parseDateFromMonthYearDay(String monthYear, String day) {
        String[] parts = monthYear.split(" ");
        String monthName = parts[0];
        int year = Integer.parseInt(parts[1]);
        int dayValue = Integer.parseInt(day);
        Month month = Month.valueOf(monthName.toUpperCase());
        return LocalDate.of(year, month, dayValue);
    }

    // Format LocalTime to 12-hour string
    public static String formatTime12Hour(LocalTime time) {
        if (time == null)
            return "";
        return time.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    // Format integer time to 12-hour string using LocalTime
    public static String formatTime12Hour(int time) {
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
}
