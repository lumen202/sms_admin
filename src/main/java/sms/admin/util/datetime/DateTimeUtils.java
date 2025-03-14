package sms.admin.util.datetime;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DateTimeUtils {
    public static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");

    public static ObservableList<String> getYearsList(int numberOfYears) {
        List<String> years = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();

        for (int i = 0; i < numberOfYears; i++) {
            String academicYear = String.format("%d-%d", currentYear - i, currentYear - i + 1);
            years.add(academicYear);
        }

        return FXCollections.observableArrayList(years);
    }

    public static String getCurrentAcademicYear() {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        // Academic year starts in July
        if (currentMonth >= 7) {
            return String.format("%d-%d", currentYear, currentYear + 1);
        } else {
            return String.format("%d-%d", currentYear - 1, currentYear);
        }
    }

    public static boolean isInAcademicYear(int targetYear, int targetMonth, String academicYear) {
        if (academicYear == null) return false;
        
        String[] years = academicYear.split("-");
        if (years.length != 2) return false;

        int startYear = Integer.parseInt(years[0]);
        int endYear = Integer.parseInt(years[1]);

        // Academic year runs from July of start year to June of end year
        if (targetYear == startYear) {
            return targetMonth >= 7; // July onwards of start year
        } else if (targetYear == endYear) {
            return targetMonth <= 6; // Up to June of end year
        }
        
        return false;
    }

    public static void updateMonthYearComboBox(ComboBox<String> comboBox, String academicYear) {
        if (comboBox == null || academicYear == null)
            return;

        try {
            int[] years = parseAcademicYear(academicYear);
            int startYear = years[0];
            int endYear = years[1];
            
            ObservableList<String> monthYears = FXCollections.observableArrayList();

            // Add months from July to December of start year
            YearMonth currentMonth = YearMonth.now();
            for (int month = 7; month <= 12; month++) {
                try {
                    YearMonth ym = YearMonth.of(startYear, month);
                    monthYears.add(formatMonthYear(ym));
                } catch (Exception e) {
                    System.err.println("Invalid month value: " + month);
                }
            }

            // Add months from January to June of end year
            for (int month = 1; month <= 6; month++) {
                try {
                    YearMonth ym = YearMonth.of(endYear, month);
                    monthYears.add(formatMonthYear(ym));
                } catch (Exception e) {
                    System.err.println("Invalid month value: " + month);
                }
            }

            comboBox.setItems(monthYears);

            // If items list is empty, try to add current month if it's in range
            if (monthYears.isEmpty()) {
                if (isMonthInAcademicYear(currentMonth, startYear, endYear)) {
                    monthYears.add(formatMonthYear(currentMonth));
                } else {
                    // Default to July of start year
                    monthYears.add(formatMonthYear(YearMonth.of(startYear, 7)));
                }
                comboBox.setItems(monthYears);
            }

            // Select appropriate month
            if (!monthYears.isEmpty()) {
                if (isMonthInAcademicYear(currentMonth, startYear, endYear) && 
                    monthYears.contains(formatMonthYear(currentMonth))) {
                    comboBox.setValue(formatMonthYear(currentMonth));
                } else {
                    comboBox.setValue(monthYears.get(0));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            comboBox.setItems(FXCollections.observableArrayList(
                formatMonthYear(YearMonth.now())
            ));
        }
    }

    private static boolean isMonthInAcademicYear(YearMonth month, int startYear, int endYear) {
        YearMonth startMonth = YearMonth.of(startYear, 7); // July of start year
        YearMonth endMonth = YearMonth.of(endYear, 6);    // June of end year
        return !month.isBefore(startMonth) && !month.isAfter(endMonth);
    }

    public static int[] parseAcademicYear(String academicYear) {
        try {
            String[] years = academicYear.split("-");
            if (years.length != 2)
                throw new IllegalArgumentException("Invalid academic year format: " + academicYear);

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

    private static boolean isValidMonthYear(int year, int month) {
        return month >= 1 && month <= 12 && year >= 1900 && year <= 9999;
    }

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

            // Validate month and year values
            if (!isValidMonthYear(year, month)) {
                System.err.println("Invalid month-year values: month=" + month + ", year=" + year);
                return YearMonth.now();
            }

            return YearMonth.of(year, month);
        } catch (Exception e) {
            System.err.println("Error parsing month-year: " + monthYear);
            e.printStackTrace();
            return YearMonth.now();
        }
    }

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

    public static String formatMonthYear(YearMonth yearMonth) {
        return yearMonth.format(MONTH_YEAR_FORMATTER);
    }
}
