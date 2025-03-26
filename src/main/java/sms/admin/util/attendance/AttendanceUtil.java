package sms.admin.util.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class AttendanceUtil {
    // Restore symbols for display
    public static final String PRESENT_MARK = "✓";
    public static final String HALF_DAY_MARK = "½";
    public static final String ABSENT_MARK = "✗";
    public static final String EXCUSED_MARK = "E";

    // Add display text for exports
    public static final String PRESENT_TEXT = "Present";
    public static final String HALF_DAY_TEXT = "Half Day";
    public static final String ABSENT_TEXT = "Absent";
    public static final String EXCUSED_TEXT = "Excused";

    // Convert marks to readable text
    public static String getDisplayText(String mark) {
        return switch (mark) {
            case PRESENT_MARK -> PRESENT_TEXT;
            case HALF_DAY_MARK -> HALF_DAY_TEXT;
            case ABSENT_MARK -> ABSENT_TEXT;
            case EXCUSED_MARK -> EXCUSED_TEXT;
            default -> ABSENT_TEXT;
        };
    }

    public static final int TIME_EXCUSED = 3000; // Changed from 9999 to 3000
    public static final int TIME_ABSENT = 0;
    public static final int TIME_IN_AM = 730;
    public static final int TIME_OUT_AM = 1130;
    public static final int TIME_IN_PM = 1300;
    public static final int TIME_OUT_PM = 1630;

    private static final Map<String, String> attendanceStatusCache = new HashMap<>();
    
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
        colFullName.setStyle("-fx-alignment: CENTER-LEFT;"); // Fixed missing quote
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public static AttendanceLog findOrCreateAttendanceLog(
            Student student,
            LocalDate date,
            ObservableList<AttendanceLog> attendanceLogs,
            ObservableList<AttendanceRecord> attendanceRecords) {

        // Find existing log
        AttendanceLog existingLog = attendanceLogs.stream()
                .filter(log -> {
                    AttendanceRecord record = log.getRecordID();
                    return log.getStudentID().equals(student) &&
                            record.getYear() == date.getYear() &&
                            record.getMonth() == date.getMonthValue() &&
                            record.getDay() == date.getDayOfMonth();
                })
                .findFirst()
                .orElse(null);

        if (existingLog != null) {
            return existingLog;
        }

        // Create new record and log
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

    public static String getAttendanceStatus(Student student, LocalDate date, ObservableList<AttendanceLog> attendanceLogs) {
        if (student == null || date == null || attendanceLogs == null) {
            return ABSENT_MARK;
        }

        String cacheKey = String.format("%d_%s", student.getStudentID(), date);
        return attendanceStatusCache.computeIfAbsent(cacheKey, k -> calculateAttendanceStatus(student, date, attendanceLogs));
    }

    private static String calculateAttendanceStatus(Student student, LocalDate date, ObservableList<AttendanceLog> attendanceLogs) {
        AttendanceLog log = attendanceLogs.stream()
            .filter(l -> isMatchingLog(l, student, date))
            .findFirst()
            .orElse(null);

        if (log == null) {
            return ABSENT_MARK;
        }

        if (isExcused(log)) return EXCUSED_MARK;
        
        boolean hasAM = hasValidTimeRange(log.getTimeInAM(), log.getTimeOutAM());
        boolean hasPM = hasValidTimeRange(log.getTimeInPM(), log.getTimeOutPM());

        return hasAM && hasPM ? PRESENT_MARK :
               hasAM || hasPM ? HALF_DAY_MARK :
                               ABSENT_MARK;
    }

    private static boolean isMatchingLog(AttendanceLog log, Student student, LocalDate date) {
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

    public static void clearCache() {
        attendanceStatusCache.clear();
    }

    public static boolean isExcused(AttendanceLog log) {
        return log.getTimeInAM() == TIME_EXCUSED &&
               log.getTimeOutAM() == TIME_EXCUSED &&
               log.getTimeInPM() == TIME_EXCUSED &&
               log.getTimeOutPM() == TIME_EXCUSED;
    }

    public static boolean isAbsent(AttendanceLog log) {
        return log.getTimeInAM() == TIME_ABSENT &&
                log.getTimeOutAM() == TIME_ABSENT &&
                log.getTimeInPM() == TIME_ABSENT &&
                log.getTimeOutPM() == TIME_ABSENT;
    }

    public static boolean isFullDayPresent(AttendanceLog log) {
        return log.getTimeInAM() > 0 && log.getTimeInAM() != TIME_EXCUSED &&
                log.getTimeOutAM() > 0 && log.getTimeOutAM() != TIME_EXCUSED &&
                log.getTimeInPM() > 0 && log.getTimeInPM() != TIME_EXCUSED &&
                log.getTimeOutPM() > 0 && log.getTimeOutPM() != TIME_EXCUSED;
    }

    public static boolean hasAnyTimeRecord(AttendanceLog log) {
        return log.getTimeInAM() > 0 || log.getTimeOutAM() > 0 ||
                log.getTimeInPM() > 0 || log.getTimeOutPM() > 0;
    }

    // Added methods from AttendanceLogUtil
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

    public static String formatTime(int time) {
        // Reuse the formatTime12Hour implementation
        return formatTime12Hour(time);
    }

    public static LocalDate parseDateFromMonthYearDay(String monthYear, String day) {
        String[] parts = monthYear.split(" ");
        String monthName = parts[0];
        int year = Integer.parseInt(parts[1]);
        int dayValue = Integer.parseInt(day);
        Month month = Month.valueOf(monthName.toUpperCase());

        return LocalDate.of(year, month, dayValue);
    }

    public static String formatTime12Hour(LocalTime time) {
        if (time == null)
            return "";
        return time.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    public static String formatTime12Hour(int time) {
        if (time == 0)
            return "--:--";
        if (time == TIME_EXCUSED)
            return "Excused"; // Changed from "EX" to "Excused"

        try {
            if (time < 0 || time > 2359) {
                return "--:--";
            }

            int hours = time / 100;
            int minutes = time % 100;

            if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
                return "--:--";
            }

            // Convert to 12-hour format
            String period = hours >= 12 ? "PM" : "AM";
            hours = hours % 12;
            if (hours == 0)
                hours = 12;

            return String.format("%02d:%02d %s", hours, minutes, period);
        } catch (Exception e) {
            return "--:--";
        }
    }

    public static String getAttendanceStatus(AttendanceLog log) {
        if (log == null) return ABSENT_MARK;

        if (isExcused(log)) return EXCUSED_MARK;

        boolean hasAM = hasValidTimeRange(log.getTimeInAM(), log.getTimeOutAM());
        boolean hasPM = hasValidTimeRange(log.getTimeInPM(), log.getTimeOutPM());

        if (hasAM && hasPM) return PRESENT_MARK;
        if (hasAM || hasPM) return HALF_DAY_MARK;
        return ABSENT_MARK;
    }
}