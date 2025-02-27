package sms.admin.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DateTimeUtils {

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

    public static void updateMonthYearComboBox(ComboBox<String> comboBox, String academicYear) {
        if (comboBox == null || academicYear == null) {
            return;
        }

        try {
            ObservableList<String> monthYears = FXCollections.observableArrayList();
            String[] years = academicYear.split("-");

            if (years.length != 2) {
                throw new IllegalArgumentException("Invalid academic year format: " + academicYear);
            }

            int startYear = Integer.parseInt(years[0].trim());
            int endYear = Integer.parseInt(years[1].trim());

            // Add months from July of start year to June of end year
            for (int year = startYear; year <= endYear; year++) {
                for (int month = 1; month <= 12; month++) {
                    // Only include months in the academic year range
                    if ((year == startYear && month >= 7) ||
                            (year == endYear && month <= 6) ||
                            (year > startYear && year < endYear)) {

                        YearMonth ym = YearMonth.of(year, month);
                        monthYears.add(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
                    }
                }
            }

            comboBox.setItems(monthYears);

            // Set to current month and year if available, otherwise first item
            YearMonth currentYM = YearMonth.now();
            String currentFormatted = currentYM.format(DateTimeFormatter.ofPattern("MMMM yyyy"));

            if (monthYears.contains(currentFormatted)) {
                comboBox.setValue(currentFormatted);
            } else {
                // If current month not in list, find closest available month
                YearMonth targetYM = currentYM;
                while (!monthYears.contains(targetYM.format(DateTimeFormatter.ofPattern("MMMM yyyy")))) {
                    targetYM = targetYM.minusMonths(1);
                }
                comboBox.setValue(targetYM.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error processing academic year: " + academicYear);
            // Set a fallback value to current month
            YearMonth current = YearMonth.now();
            comboBox.setItems(FXCollections.observableArrayList(
                    current.format(DateTimeFormatter.ofPattern("MMMM yyyy"))));
            comboBox.setValue(comboBox.getItems().get(0));
        }
    }
}
