package sms.admin.app.attendance.dialog;

import javafx.util.StringConverter;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

public class DayOfWeekStringConverter extends StringConverter<DayOfWeek> {
    @Override
    public String toString(DayOfWeek day) {
        if (day == null) return "";
        return day.getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    @Override
    public DayOfWeek fromString(String string) {
        if (string == null || string.isEmpty()) return null;
        return DayOfWeek.valueOf(string.toUpperCase());
    }
}
