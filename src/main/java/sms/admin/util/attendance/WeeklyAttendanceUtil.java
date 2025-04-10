package sms.admin.util.attendance;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;

public class WeeklyAttendanceUtil {
    private static final String WEEK_RANGE_SEPARATOR = " - ";
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd");
    private static final double DEFAULT_DAY_COLUMN_WIDTH = 120.0;

    private static final Map<String, Integer> workingDaysCache = new HashMap<>();
    private static final Map<String, Double> weekWidthCache = new HashMap<>();

    private static String generateWeekKey(WeekDates week) {
        return week.getStart().toString() + "_" + week.getEnd().toString();
    }

    public static LocalDate getFirstDayOfMonth(String selectedMonthYear) {
        String[] parts = selectedMonthYear.split(" ");
        String monthName = parts[0];
        int yearNumber = Integer.parseInt(parts[1]);
        Month month = Month.valueOf(monthName.toUpperCase());
        LocalDate firstDay = LocalDate.of(yearNumber, month.getValue(), 1);

        // Skip to first weekday
        while (CommonAttendanceUtil.isWeekend(firstDay)) {
            firstDay = firstDay.plusDays(1);
        }
        return firstDay;
    }

    public static LocalDate findWeekEndDate(LocalDate start, int month) {
        LocalDate current = start;
        while (current.getMonthValue() == month) {
            if (current.getDayOfWeek().getValue() == 5 ||
                    current.plusDays(1).getMonthValue() != month) {
                return current;
            }
            current = current.plusDays(1);
        }
        return null;
    }

    public static TableColumn<Student, String> createDayColumn(
            LocalDate date,
            ObservableList<AttendanceLog> attendanceLogs) {
        // Use TableColumnUtil instead
        return TableColumnUtil.createDayColumn(date, attendanceLogs, DEFAULT_DAY_COLUMN_WIDTH);
    }

    public static void updateWeeklyColumns(
            ObservableList<TableColumn<Student, ?>> weekColumns,
            String selectedMonthYear,
            String selectedWeek,
            ObservableList<AttendanceLog> attendanceLogs) {

        if (selectedMonthYear == null || selectedWeek == null || weekColumns == null) {
            return;
        }

        String[] parts = selectedMonthYear.split(" ");
        if (parts.length < 2)
            return;

        String monthName = parts[0];
        int yearNumber = Integer.parseInt(parts[1]);
        Month month = Month.valueOf(monthName.toUpperCase());

        String[] weekRange = selectedWeek.split(WEEK_RANGE_SEPARATOR);
        if (weekRange.length < 2)
            return;

        int startDay = Integer.parseInt(weekRange[0]);
        int endDay = Integer.parseInt(weekRange[1]);

        LocalDate date = LocalDate.of(yearNumber, month.getValue(), startDay);
        LocalDate endDate = LocalDate.of(yearNumber, month.getValue(), endDay);

        weekColumns.clear();
        while (!date.isAfter(endDate)) {
            if (!CommonAttendanceUtil.isWeekend(date)) {
                TableColumn<Student, String> dayColumn = createDayColumn(date, attendanceLogs);
                if (dayColumn != null) {
                    weekColumns.add(dayColumn);
                }
            }
            date = date.plusDays(1);
        }
    }

    public static void populateWeekComboBox(ComboBox<String> weekComboBox, LocalDate firstDayOfMonth) {
        LocalDate currentDay = firstDayOfMonth;
        int currentMonth = firstDayOfMonth.getMonthValue();

        while (currentDay.getMonthValue() == currentMonth) {
            LocalDate weekEnd = findWeekEndDate(currentDay, currentMonth);
            if (weekEnd != null) {
                String weekRange = currentDay.format(DAY_FORMATTER) +
                        WEEK_RANGE_SEPARATOR +
                        weekEnd.format(DAY_FORMATTER);
                weekComboBox.getItems().add(weekRange);

                currentDay = weekEnd.plusDays(1);
                while (currentDay.getMonthValue() == currentMonth &&
                        CommonAttendanceUtil.isWeekend(currentDay)) {
                    currentDay = currentDay.plusDays(1);
                }
            } else {
                break;
            }
        }
    }

    public static void setDefaultWeek(ComboBox<String> weekComboBox, int monthNumber, int yearNumber) {
        if (weekComboBox.getItems().isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();

        // Only set current week if we're in the current month
        if (monthNumber == today.getMonthValue() && yearNumber == today.getYear()) {
            // Find the week that contains today
            for (String weekRange : weekComboBox.getItems()) {
                String[] range = weekRange.split(WEEK_RANGE_SEPARATOR);
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);

                LocalDate weekStart = LocalDate.of(yearNumber, monthNumber, start);
                LocalDate weekEnd = LocalDate.of(yearNumber, monthNumber, end);

                if (!today.isBefore(weekStart) && !today.isAfter(weekEnd)) {
                    weekComboBox.setValue(weekRange);
                    return;
                }
            }
        }

        // If not current month or week not found, select first week
        weekComboBox.setValue(weekComboBox.getItems().get(0));
    }

    public static List<WeekDates> splitIntoWeeks(LocalDate startDate, LocalDate endDate) {
        List<WeekDates> weeks = new ArrayList<>();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        LocalDate current = startDate;
        LocalDate weekStart = current;
        int currentWeek = current.get(weekFields.weekOfWeekBasedYear());

        while (!current.isAfter(endDate)) {
            if (current.get(weekFields.weekOfWeekBasedYear()) != currentWeek) {
                weeks.add(new WeekDates(weekStart, current.minusDays(1)));
                weekStart = current;
                currentWeek = current.get(weekFields.weekOfWeekBasedYear());
            }
            current = current.plusDays(1);
        }

        // Add the last week
        if (!weekStart.isAfter(endDate)) {
            weeks.add(new WeekDates(weekStart, endDate));
        }

        return weeks;
    }

    public static int calculateWorkingDays(WeekDates week) {
        String key = generateWeekKey(week);
        return workingDaysCache.computeIfAbsent(key, k -> (int) week.getDates().stream()
                .filter(date -> !CommonAttendanceUtil.isWeekend(date))
                .count());
    }

    public static double calculateWeekWidth(int workingDaysInWeek, double totalWidth, int totalWorkingDays) {
        String key = workingDaysInWeek + "_" + totalWidth + "_" + totalWorkingDays;
        return weekWidthCache.computeIfAbsent(key, k -> workingDaysInWeek * totalWidth / Math.max(totalWorkingDays, 1));
    }

    public static void clearCaches() {
        workingDaysCache.clear();
        weekWidthCache.clear();
    }

    public static class WeekDates {
        private final LocalDate start;
        private final LocalDate end;

        public WeekDates(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        public LocalDate getStart() {
            return start;
        }

        public LocalDate getEnd() {
            return end;
        }

        public List<LocalDate> getDates() {
            List<LocalDate> dates = new ArrayList<>();
            LocalDate current = start;
            while (!current.isAfter(end)) {
                if (!CommonAttendanceUtil.isWeekend(current)) {
                    dates.add(current);
                }
                current = current.plusDays(1);
            }
            dates.sort(LocalDate::compareTo); // Ensure dates are sorted
            return dates;
        }

        public boolean hasWorkingDays() {
            return getDates().stream()
                    .anyMatch(date -> !CommonAttendanceUtil.isWeekend(date));
        }

        public WeekDates filterDatesInRange(LocalDate startDate, LocalDate endDate) {
            List<LocalDate> filteredDates = getDates().stream()
                    .filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
                    .collect(Collectors.toList());

            return new WeekDates(
                    filteredDates.isEmpty() ? start : filteredDates.get(0),
                    filteredDates.isEmpty() ? end : filteredDates.get(filteredDates.size() - 1));
        }
    }
}