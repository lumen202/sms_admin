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

/**
 * Utility class for managing weekly attendance data and UI components in the
 * attendance system.
 * Provides methods for handling week-based attendance calculations, table
 * column management,
 * and week selection in JavaFX UI components.
 */
public class WeeklyAttendanceUtil {
    private static final String WEEK_RANGE_SEPARATOR = " - ";
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd");
    private static final double DEFAULT_DAY_COLUMN_WIDTH = 120.0;

    private static final Map<String, Integer> workingDaysCache = new HashMap<>();
    private static final Map<String, Double> weekWidthCache = new HashMap<>();

    /**
     * Generates a unique key for caching week data based on start and end dates.
     *
     * @param week The WeekDates object containing start and end dates
     * @return A string key combining start and end dates
     */
    private static String generateWeekKey(WeekDates week) {
        return week.getStart().toString() + "_" + week.getEnd().toString();
    }

    /**
     * Determines the first working day of a specified month and year.
     *
     * @param selectedMonthYear String in format "Month Year" (e.g., "January 2023")
     * @return LocalDate representing the first non-weekend day of the month
     */
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

    /**
     * Finds the end date of a week within the same month, ending on Friday
     * or the last day of the month.
     *
     * @param start The start date of the week
     * @param month The month number (1-12)
     * @return LocalDate representing the week's end date, or null if not found
     */
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

    /**
     * Creates a table column for a specific date's attendance data.
     *
     * @param date           The date for the column
     * @param attendanceLogs ObservableList of attendance logs
     * @return TableColumn configured for displaying attendance data
     */
    public static TableColumn<Student, String> createDayColumn(
            LocalDate date,
            ObservableList<AttendanceLog> attendanceLogs) {
        return TableColumnUtil.createDayColumn(date, attendanceLogs, DEFAULT_DAY_COLUMN_WIDTH);
    }

    /**
     * Updates the weekly attendance columns in a TableView based on selected month
     * and week.
     *
     * @param weekColumns       ObservableList of TableColumn to be updated
     * @param selectedMonthYear String in format "Month Year"
     * @param selectedWeek      String in format "dd - dd" representing week range
     * @param attendanceLogs    ObservableList of attendance logs
     */
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

    /**
     * Populates a ComboBox with week ranges for a given month.
     *
     * @param weekComboBox    The ComboBox to populate
     * @param firstDayOfMonth The first working day of the month
     */
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

    /**
     * Sets the default selected week in a ComboBox based on current date or first
     * week.
     *
     * @param weekComboBox The ComboBox containing week ranges
     * @param monthNumber  The month number (1-12)
     * @param yearNumber   The year
     */
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

    /**
     * Splits a date range into weekly periods.
     *
     * @param startDate The start date of the range
     * @param endDate   The end date of the range
     * @return List of WeekDates objects representing each week
     */
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

    /**
     * Calculates the number of working days in a week, using cache for performance.
     *
     * @param week The WeekDates object representing the week
     * @return Number of non-weekend days in the week
     */
    public static int calculateWorkingDays(WeekDates week) {
        String key = generateWeekKey(week);
        return workingDaysCache.computeIfAbsent(key, k -> (int) week.getDates().stream()
                .filter(date -> !CommonAttendanceUtil.isWeekend(date))
                .count());
    }

    /**
     * Calculates the proportional width of a week based on working days.
     *
     * @param workingDaysInWeek Number of working days in the week
     * @param totalWidth        Total available width
     * @param totalWorkingDays  Total working days in the period
     * @return Calculated width for the week
     */
    public static double calculateWeekWidth(int workingDaysInWeek, double totalWidth, int totalWorkingDays) {
        String key = workingDaysInWeek + "_" + totalWidth + "_" + totalWorkingDays;
        return weekWidthCache.computeIfAbsent(key, k -> workingDaysInWeek * totalWidth / Math.max(totalWorkingDays, 1));
    }

    /**
     * Clears all cached data for working days and week widths.
     */
    public static void clearCaches() {
        workingDaysCache.clear();
        weekWidthCache.clear();
    }

    /**
     * Inner class representing a week's date range.
     */
    public static class WeekDates {
        private final LocalDate start;
        private final LocalDate end;

        /**
         * Constructs a WeekDates object with start and end dates.
         *
         * @param start The start date of the week
         * @param end   The end date of the week
         */
        public WeekDates(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        /**
         * Gets the start date of the week.
         *
         * @return LocalDate representing the start date
         */
        public LocalDate getStart() {
            return start;
        }

        /**
         * Gets the end date of the week.
         *
         * @return LocalDate representing the end date
         */
        public LocalDate getEnd() {
            return end;
        }

        /**
         * Gets all working dates in the week.
         *
         * @return List of LocalDate objects (excluding weekends)
         */
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

        /**
         * Checks if the week contains any working days.
         *
         * @return true if there are non-weekend days, false otherwise
         */
        public boolean hasWorkingDays() {
            return getDates().stream()
                    .anyMatch(date -> !CommonAttendanceUtil.isWeekend(date));
        }

        /**
         * Filters dates to those within a specified range.
         *
         * @param startDate The start of the range
         * @param endDate   The end of the range
         * @return New WeekDates object with filtered dates
         */
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