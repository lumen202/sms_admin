package sms.admin.util.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;

public class CommonAttendanceUtil {
    public static final String PRESENT_MARK = "✓";
    public static final String HALF_DAY_MARK = "½";
    public static final String ABSENT_MARK = "✗";
    public static final String EXCUSED_MARK = "E";

    public static final String PRESENT_TEXT = "Present";
    public static final String HALF_DAY_TEXT = "Half Day";
    public static final String ABSENT_TEXT = "Absent"; 
    public static final String EXCUSED_TEXT = "Excused";

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
        DayOfWeek.SUNDAY, "Su"
    );

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
        if (log == null) return ABSENT_MARK;
        
        if (isExcused(log)) return EXCUSED_MARK;
        
        boolean hasAM = hasValidTimeRange(log.getTimeInAM(), log.getTimeOutAM());
        boolean hasPM = hasValidTimeRange(log.getTimeInPM(), log.getTimeOutPM());
        
        if (hasAM && hasPM) return PRESENT_MARK;
        if (hasAM || hasPM) return HALF_DAY_MARK;
        return ABSENT_MARK;
    }

    private static boolean hasValidTimeRange(int timeIn, int timeOut) {
        return timeIn > 0 && timeIn != TIME_ABSENT && 
               timeOut > 0 && timeOut != TIME_ABSENT;
    }
}
