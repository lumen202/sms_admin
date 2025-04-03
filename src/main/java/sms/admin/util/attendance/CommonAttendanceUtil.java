package sms.admin.util.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;

public class CommonAttendanceUtil {
    public static final String PRESENT_MARK = "✓";
    public static final String HALF_DAY_MARK = "½";
    public static final String ABSENT_MARK = "✗";
    public static final String EXCUSED_MARK = "E";
    public static final String HOLIDAY_MARK = "H";

    public static final String PRESENT_TEXT = "Present";
    public static final String HALF_DAY_TEXT = "Half Day";
    public static final String ABSENT_TEXT = "Absent";
    public static final String EXCUSED_TEXT = "Excused";
    public static final String HOLIDAY_TEXT = "Holiday";

    public static final int TIME_HOLIDAY = -2;
    public static final int TIME_EXCUSED = 3000;
    public static final int TIME_ABSENT = 0;
    public static final int TIME_IN_AM = 730;
    public static final int TIME_OUT_AM = 1130;
    public static final int TIME_IN_PM = 1300;
    public static final int TIME_OUT_PM = 1630;

    private static final Map<DayOfWeek, String> DAY_INITIALS = Map.of(
            DayOfWeek.MONDAY, "M",
            DayOfWeek.TUESDAY, "T",
            DayOfWeek.WEDNESDAY, "W",
            DayOfWeek.THURSDAY, "Th",
            DayOfWeek.FRIDAY, "F",
            DayOfWeek.SATURDAY, "Sa",
            DayOfWeek.SUNDAY, "Su");

    private static final Set<LocalDate> HOLIDAY_DATES = new HashSet<>();

    public static boolean isWeekend(LocalDate date) {
        return date != null && (date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == DayOfWeek.SUNDAY);
    }

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

    public static boolean isExcused(AttendanceLog log) {
        return log.getTimeInAM() == TIME_EXCUSED &&
                log.getTimeOutAM() == TIME_EXCUSED &&
                log.getTimeInPM() == TIME_EXCUSED &&
                log.getTimeOutPM() == TIME_EXCUSED;
    }

    public static boolean isMatchingLog(AttendanceLog log, Student student, LocalDate date) {
        return log != null &&
                log.getStudentID() != null &&
                log.getRecordID() != null &&
                log.getStudentID().getStudentID() == student.getStudentID() &&
                log.getRecordID().getYear() == date.getYear() &&
                log.getRecordID().getMonth() == date.getMonthValue() &&
                log.getRecordID().getDay() == date.getDayOfMonth();
    }

    public static String getDayInitial(DayOfWeek day) {
        return DAY_INITIALS.getOrDefault(day, "");
    }

    public static String computeAttendanceStatus(AttendanceLog log) {
        if (log == null) {
            System.out.println("Computing status: log is null -> " + ABSENT_MARK);
            return ABSENT_MARK;
        }

        System.out.println("Computing status for LogID: " + log.getLogID());
        System.out.println("Times - AM: " + log.getTimeInAM() + "/" + log.getTimeOutAM() + 
                         ", PM: " + log.getTimeInPM() + "/" + log.getTimeOutPM());

        if (isHoliday(log)) {
            System.out.println("Status: Holiday detected -> " + HOLIDAY_MARK);
            return HOLIDAY_MARK;
        }
            
        if (isExcused(log)) {
            System.out.println("Status: Excused detected -> " + EXCUSED_MARK);
            return EXCUSED_MARK;
        }

        boolean hasAM = hasValidTimeRange(log.getTimeInAM(), log.getTimeOutAM());
        boolean hasPM = hasValidTimeRange(log.getTimeInPM(), log.getTimeOutPM());

        System.out.println("Time ranges - AM valid: " + hasAM + ", PM valid: " + hasPM);

        if (hasAM && hasPM) {
            System.out.println("Status: Both AM/PM valid -> " + PRESENT_MARK);
            return PRESENT_MARK;
        }
        if (hasAM || hasPM) {
            System.out.println("Status: Half day detected -> " + HALF_DAY_MARK);
            return HALF_DAY_MARK;
        }

        System.out.println("Status: Default absent -> " + ABSENT_MARK);
        return ABSENT_MARK;
    }

    public static boolean isHoliday(AttendanceLog log) {
        return log != null && 
               log.getTimeInAM() == TIME_HOLIDAY &&
               log.getTimeOutAM() == TIME_HOLIDAY &&
               log.getTimeInPM() == TIME_HOLIDAY &&
               log.getTimeOutPM() == TIME_HOLIDAY;
    }

    public static boolean isHolidayDate(LocalDate date) {
        return HOLIDAY_DATES.contains(date);
    }

    public static void addHolidayDate(LocalDate date) {
        HOLIDAY_DATES.add(date);
    }

    public static void removeHolidayDate(LocalDate date) {
        HOLIDAY_DATES.remove(date);
    }

    private static boolean hasValidTimeRange(int timeIn, int timeOut) {
        return timeIn > 0 && timeIn != TIME_ABSENT &&
                timeOut > 0 && timeOut != TIME_ABSENT;
    }
}
