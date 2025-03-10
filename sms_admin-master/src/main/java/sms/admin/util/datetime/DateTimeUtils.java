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
            String[] years = academicYear.split("-");
            if (years.length != 2)
                throw new IllegalArgumentException("Invalid academic year format: " + academicYear);

            int startYear = Integer.parseInt(years[0].trim());
            int endYear = Integer.parseInt(years[1].trim());
            
            ObservableList<String> monthYears = FXCollections.observableArrayList();

            // Add months from July to December of start year
            for (int month = 7; month <= 12; month++) {
                YearMonth ym = YearMonth.of(startYear, month);
                monthYears.add(ym.format(MONTH_YEAR_FORMATTER));
            }

            // Add months from January to June of end year
            for (int month = 1; month <= 6; month++) {
                YearMonth ym = YearMonth.of(endYear, month);
                monthYears.add(ym.format(MONTH_YEAR_FORMATTER));
            }

            comboBox.setItems(monthYears);
            
            // Set current month if it falls within the academic year
            YearMonth current = YearMonth.now();
            String currentFormatted = current.format(MONTH_YEAR_FORMATTER);
            
            // If current month is in the list, use it; otherwise use first month
            if (monthYears.contains(currentFormatted)) {
                comboBox.setValue(currentFormatted);
            } else {
                comboBox.setValue(monthYears.get(0));
            }

        } catch (Exception e) {
            e.printStackTrace();
            comboBox.setItems(FXCollections.observableArrayList(
                    YearMonth.now().format(MONTH_YEAR_FORMATTER)));
            comboBox.setValue(comboBox.getItems().get(0));
        }
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

    public static YearMonth parseMonthYear(String monthYear) {
        try {
            return YearMonth.parse(monthYear, MONTH_YEAR_FORMATTER);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    public static String formatMonthYear(YearMonth yearMonth) {
        return yearMonth.format(MONTH_YEAR_FORMATTER);
    }
}
