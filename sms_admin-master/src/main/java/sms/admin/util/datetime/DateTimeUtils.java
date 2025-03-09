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
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");

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
        if (comboBox == null || academicYear == null)
            return;

        try {
            String[] years = academicYear.split("-");
            if (years.length != 2)
                throw new IllegalArgumentException("Invalid academic year: " + academicYear);

            int startYear = Integer.parseInt(years[0].trim());
            int endYear = Integer.parseInt(years[1].trim());
            ObservableList<String> monthYears = FXCollections.observableArrayList();
            YearMonth current = YearMonth.now();

            YearMonth ym = YearMonth.of(startYear, 7); // Start from July
            YearMonth endDate = YearMonth.of(endYear, 6); // End in June

            while (!ym.isAfter(endDate)) {
                monthYears.add(ym.format(MONTH_YEAR_FORMATTER));
                ym = ym.plusMonths(1);
            }

            comboBox.setItems(monthYears);
            comboBox.setValue(
                    monthYears.contains(current.format(MONTH_YEAR_FORMATTER)) ? current.format(MONTH_YEAR_FORMATTER)
                            : monthYears.get(0));

        } catch (Exception e) {
            e.printStackTrace();
            comboBox.setItems(FXCollections.observableArrayList(
                    YearMonth.now().format(MONTH_YEAR_FORMATTER)));
            comboBox.setValue(comboBox.getItems().get(0));
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
