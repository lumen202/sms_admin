/**
 * Utility class for handling date and time operations related to academic years
 * and month-year formatting within the SMS administrative module.
 * <p>
 * Provides methods for generating lists of academic years, determining the current
 * academic year, parsing and formatting month-year strings, and populating JavaFX
 * ComboBox controls with appropriate month-year values based on an academic year.
 * </p>
 */
package sms.admin.util.datetime;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DateTimeUtils {
    /**
     * Formatter for displaying a YearMonth as "MMMM yyyy", e.g., "January 2025".
     */
    public static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");

    /**
     * Generates an observable list of academic year strings for a given number of
     * past years.
     * <p>
     * Each academic year is formatted as "YYYY-YYYY", starting from the current
     * calendar year
     * and going backwards.
     * </p>
     *
     * @param numberOfYears the number of academic years to generate
     * @return ObservableList of academic year strings
     */
    public static ObservableList<String> getYearsList(int numberOfYears) {
        List<String> years = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();

        for (int i = 0; i < numberOfYears; i++) {
            String academicYear = String.format("%d-%d",
                    currentYear - i,
                    currentYear - i + 1);
            years.add(academicYear);
        }

        return FXCollections.observableArrayList(years);
    }

    /**
     * Determines the current academic year based on today's date.
     * <p>
     * The academic year is assumed to start in July and end in June.
     * If the current month is July or later, the academic year is
     * the current calendar year to next year; otherwise it is the
     * previous year to the current year.
     * </p>
     *
     * @return String representation of the current academic year, "YYYY-YYYY"
     */
    public static String getCurrentAcademicYear() {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        if (currentMonth >= 7) {
            return String.format("%d-%d", currentYear, currentYear + 1);
        } else {
            return String.format("%d-%d", currentYear - 1, currentYear);
        }
    }

    /**
     * Checks if a given year-month falls within a specified academic year.
     * <p>
     * The academic year runs from July of the start year to June of the end year.
     * </p>
     *
     * @param targetYear   the year to check
     * @param targetMonth  the month to check (1-12)
     * @param academicYear academic year string "YYYY-YYYY"
     * @return true if the date is within the academic year, false otherwise
     */
    public static boolean isInAcademicYear(int targetYear,
            int targetMonth,
            String academicYear) {
        if (academicYear == null)
            return false;

        String[] years = academicYear.split("-");
        if (years.length != 2)
            return false;

        int startYear = Integer.parseInt(years[0]);
        int endYear = Integer.parseInt(years[1]);

        if (targetYear == startYear) {
            return targetMonth >= 7;
        } else if (targetYear == endYear) {
            return targetMonth <= 6;
        }
        return false;
    }

    /**
     * Populates a ComboBox with month-year values for a given academic year,
     * and selects the current month if it falls within that academic year.
     * <p>
     * Months from July of the start year through June of the end year are added.
     * If no months match, it defaults to July of the start year or the current
     * month
     * if within range.
     * </p>
     *
     * @param comboBox     the ComboBox to populate
     * @param academicYear academic year string "YYYY-YYYY"
     */
    public static void updateMonthYearComboBox(ComboBox<String> comboBox,
            String academicYear) {
        if (comboBox == null || academicYear == null)
            return;

        try {
            int[] years = parseAcademicYear(academicYear);
            int startYear = years[0];
            int endYear = years[1];
            YearMonth currentMonth = YearMonth.now();

            ObservableList<String> monthYears = FXCollections.observableArrayList();

            // July-December of start year
            for (int month = 7; month <= 12; month++) {
                try {
                    YearMonth ym = YearMonth.of(startYear, month);
                    monthYears.add(formatMonthYear(ym));
                } catch (Exception e) {
                    System.err.println("Invalid month value: " + month);
                }
            }
            // January-June of end year
            for (int month = 1; month <= 6; month++) {
                try {
                    YearMonth ym = YearMonth.of(endYear, month);
                    monthYears.add(formatMonthYear(ym));
                } catch (Exception e) {
                    System.err.println("Invalid month value: " + month);
                }
            }

            comboBox.setItems(monthYears);

            if (monthYears.isEmpty()) {
                if (isMonthInAcademicYear(currentMonth, startYear, endYear)) {
                    monthYears.add(formatMonthYear(currentMonth));
                } else {
                    monthYears.add(formatMonthYear(YearMonth.of(startYear, 7)));
                }
                comboBox.setItems(monthYears);
            }

            if (!monthYears.isEmpty()) {
                String formattedCurrent = formatMonthYear(currentMonth);
                if (isMonthInAcademicYear(currentMonth, startYear, endYear)
                        && monthYears.contains(formattedCurrent)) {
                    comboBox.setValue(formattedCurrent);
                } else {
                    comboBox.setValue(monthYears.get(0));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            comboBox.setItems(FXCollections.observableArrayList(
                    formatMonthYear(YearMonth.now())));
        }
    }

    /**
     * Checks if a YearMonth falls within the academic year range.
     *
     * @param month     the YearMonth to check
     * @param startYear the start academic year
     * @param endYear   the end academic year
     * @return true if within range, false otherwise
     */
    private static boolean isMonthInAcademicYear(YearMonth month,
            int startYear,
            int endYear) {
        YearMonth startMonth = YearMonth.of(startYear, 7);
        YearMonth endMonth = YearMonth.of(endYear, 6);
        return !month.isBefore(startMonth) && !month.isAfter(endMonth);
    }

    /**
     * Parses an academic year string into start and end years.
     *
     * @param academicYear academic year string "YYYY-YYYY"
     * @return int array [startYear, endYear]
     */
    public static int[] parseAcademicYear(String academicYear) {
        try {
            String[] years = academicYear.split("-");
            if (years.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid academic year format: " + academicYear);
            }
            return new int[] {
                    Integer.parseInt(years[0].trim()),
                    Integer.parseInt(years[1].trim())
            };
        } catch (Exception e) {
            e.printStackTrace();
            int currentYear = LocalDate.now().getYear();
            return new int[] { currentYear, currentYear + 1 };
        }
    }

    /**
     * Validates a month-year combination.
     *
     * @param year  the year (>=1900, <=9999)
     * @param month the month (1-12)
     * @return true if valid, false otherwise
     */
    private static boolean isValidMonthYear(int year, int month) {
        return month >= 1 && month <= 12 && year >= 1900 && year <= 9999;
    }

    /**
     * Parses a "MMMM yyyy" string into a YearMonth.
     * <p>
     * Falls back to the current month if parsing fails.
     * </p>
     *
     * @param monthYear string to parse (e.g., "March 2025")
     * @return corresponding YearMonth, or current month on error
     */
    public static YearMonth parseMonthYear(String monthYear) {
        if (monthYear == null || monthYear.trim().isEmpty()) {
            return YearMonth.now();
        }
        try {
            String[] parts = monthYear.trim().split("\\s+");
            if (parts.length != 2) {
                System.err.println("Invalid month-year format: " + monthYear);
                return YearMonth.now();
            }
            int month = parseMonth(parts[0].trim());
            int year = Integer.parseInt(parts[1].trim());
            if (!isValidMonthYear(year, month)) {
                System.err.println("Invalid month-year values: month=" + month
                        + ", year=" + year);
                return YearMonth.now();
            }
            return YearMonth.of(year, month);
        } catch (Exception e) {
            System.err.println("Error parsing month-year: " + monthYear);
            e.printStackTrace();
            return YearMonth.now();
        }
    }

    /**
     * Converts a month name to its integer representation.
     *
     * @param monthStr full or abbreviated month name (case-insensitive)
     * @return integer month (1-12)
     * @throws IllegalArgumentException if the month name is unrecognized
     */
    private static int parseMonth(String monthStr) {
        return switch (monthStr.toLowerCase()) {
            case "january", "jan" -> 1;
            case "february", "feb" -> 2;
            case "march", "mar" -> 3;
            case "april", "apr" -> 4;
            case "may" -> 5;
            case "june", "jun" -> 6;
            case "july", "jul" -> 7;
            case "august", "aug" -> 8;
            case "september", "sep" -> 9;
            case "october", "oct" -> 10;
            case "november", "nov" -> 11;
            case "december", "dec" -> 12;
            default -> throw new IllegalArgumentException("Invalid month: " + monthStr);
        };
    }

    /**
     * Formats a YearMonth using the {@link #MONTH_YEAR_FORMATTER}.
     *
     * @param yearMonth the YearMonth to format
     * @return formatted string "MMMM yyyy"
     */
    public static String formatMonthYear(YearMonth yearMonth) {
        return yearMonth.format(MONTH_YEAR_FORMATTER);
    }
}