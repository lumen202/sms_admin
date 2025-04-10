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

public class CommonAttendanceUtil {
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

    // Holiday management methods
    public static void addHolidayDate(LocalDate date) {
        holidayDates.add(date);
    }

    public static void removeHolidayDate(LocalDate date) {
        holidayDates.remove(date);
    }

    public static boolean isHolidayDate(LocalDate date) {
        return holidayDates.contains(date);
    }

    // Table column setup
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

    // Date and time utility methods
    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

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

    // Attendance log management
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

    // Status computation methods
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

    // Status check methods
    public static boolean isExcused(AttendanceLog log) {
        return log != null &&
                log.getTimeInAM() == TIME_EXCUSED &&
                log.getTimeOutAM() == TIME_EXCUSED &&
                log.getTimeInPM() == TIME_EXCUSED &&
                log.getTimeOutPM() == TIME_EXCUSED;
    }

    public static boolean isHoliday(AttendanceLog log) {
        return log != null &&
                log.getTimeInAM() == TIME_HOLIDAY &&
                log.getTimeOutAM() == TIME_HOLIDAY &&
                log.getTimeInPM() == TIME_HOLIDAY &&
                log.getTimeOutPM() == TIME_HOLIDAY;
    }

    public static boolean isAbsent(AttendanceLog log) {
        return log != null &&
                log.getTimeInAM() == TIME_ABSENT &&
                log.getTimeOutAM() == TIME_ABSENT &&
                log.getTimeInPM() == TIME_ABSENT &&
                log.getTimeOutPM() == TIME_ABSENT;
    }

    // Utility methods
    public static boolean isMatchingLog(AttendanceLog log, Student student, LocalDate date) {
        return log != null &&
                log.getStudentID() != null &&
                log.getRecordID() != null &&
                log.getStudentID().getStudentID() == student.getStudentID() &&
                log.getRecordID().getYear() == date.getYear() &&
                log.getRecordID().getMonth() == date.getMonthValue() &&
                log.getRecordID().getDay() == date.getDayOfMonth();
    }

    private static boolean hasValidTimeRange(int timeIn, int timeOut) {
        return timeIn > 0 && timeIn != TIME_ABSENT &&
                timeOut > 0 && timeOut != TIME_ABSENT;
    }

    public static void clearAttendanceCache() {
        attendanceStatusCache.clear();
    }

    public static String formatTime(int time) {
        return formatTime12Hour(time);
    }

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

    public static String formatTime12Hour(LocalTime time) {
        if (time == null)
            return "";
        return time.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }
}
