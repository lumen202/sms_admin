package sms.admin.util.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import javafx.scene.control.TableColumn;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import dev.finalproject.models.Student;

public class AttendanceDateUtil {
    
    public static String formatDayName(DayOfWeek day, boolean useShortName) {
        if (useShortName) {
            return switch (day) {
                case MONDAY -> "M";
                case TUESDAY -> "T";
                case WEDNESDAY -> "W";
                case THURSDAY -> "Th";
                case FRIDAY -> "F";
                default -> day.getDisplayName(TextStyle.SHORT, Locale.getDefault());
            };
        }
        return day.getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    public static Map<DayOfWeek, TableColumn<Student, String>> createDayNameColumns(boolean useShortNames) {
        Map<DayOfWeek, TableColumn<Student, String>> columns = new LinkedHashMap<>();
        
        DayOfWeek[] weekdays = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        };
        
        for (DayOfWeek day : weekdays) {
            String dayName = formatDayName(day, useShortNames);
            TableColumn<Student, String> dayColumn = new TableColumn<>(dayName);
            dayColumn.setStyle("-fx-alignment: CENTER;");
            columns.put(day, dayColumn);
        }
        
        return columns;
    }

    public static Map<DayOfWeek, TableColumn<Student, String>> createDayNameColumns(boolean useShortNames, List<LocalDate> datesInWeek) {
        Map<DayOfWeek, TableColumn<Student, String>> columns = new LinkedHashMap<>();
        
        datesInWeek.stream()
                   .filter(date -> !isWeekend(date))
                   .map(LocalDate::getDayOfWeek)
                   .distinct()
                   .forEach(day -> {
                       String dayName = formatDayName(day, useShortNames);
                       TableColumn<Student, String> dayColumn = new TableColumn<>(dayName);
                       dayColumn.setStyle("-fx-alignment: CENTER;");
                       dayColumn.setUserData(day); // Store day of week for reference
                       columns.put(day, dayColumn);
                   });
        
        return columns;
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || 
               date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
