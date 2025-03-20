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
import java.util.Arrays;

public class AttendanceDateUtil {
    private static final DayOfWeek[] WEEKDAYS = {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    };

    private static final Map<DayOfWeek, String> SHORT_NAMES = Map.of(
        DayOfWeek.MONDAY, "M",
        DayOfWeek.TUESDAY, "T",
        DayOfWeek.WEDNESDAY, "W",
        DayOfWeek.THURSDAY, "Th",
        DayOfWeek.FRIDAY, "F"
    );

    public static String formatDayName(DayOfWeek day, boolean useShortName) {
        return useShortName ? SHORT_NAMES.getOrDefault(day, 
            day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) :
            day.getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    public static Map<DayOfWeek, TableColumn<Student, String>> createDayNameColumns(boolean useShortNames, List<LocalDate> dates) {
        Map<DayOfWeek, TableColumn<Student, String>> columns = new LinkedHashMap<>();
        
        (dates != null ? dates.stream()
                              .filter(date -> !isWeekend(date))
                              .map(LocalDate::getDayOfWeek)
                              .distinct()
                              .sorted() :
                        Arrays.stream(WEEKDAYS))
            .forEach(day -> {
                String dayName = formatDayName(day, useShortNames);
                TableColumn<Student, String> dayColumn = new TableColumn<>(dayName);
                dayColumn.setStyle("-fx-alignment: CENTER;");
                columns.put(day, dayColumn);
            });
        
        return columns;
    }

    public static boolean isWeekend(LocalDate date) {
        return date != null && (date.getDayOfWeek() == DayOfWeek.SATURDAY || 
                              date.getDayOfWeek() == DayOfWeek.SUNDAY);
    }
}
