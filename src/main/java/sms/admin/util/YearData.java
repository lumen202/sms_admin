package sms.admin.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.time.Month;

public class YearData {
    public static ObservableList<String> getYears() {
        ObservableList<String> years = FXCollections.observableArrayList();
        int academicYear = getCurrentStartYear();

        // Add academic years starting from current academic year
        for (int i = 0; i < 5; i++) {
            years.add((academicYear + i) + "-" + (academicYear + i + 1));
        }
        return years;
    }

    public static String getCurrentAcademicYear() {
        int startYear = getCurrentStartYear();
        return startYear + "-" + (startYear + 1);
    }

    private static int getCurrentStartYear() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        // If we're in the latter half of the year (June onwards),
        // we're in the academic year that started this year.
        // Otherwise, we're in the academic year that started last year.
        if (today.getMonth().getValue() < Month.JUNE.getValue()) {
            return currentYear - 1;
        }
        return currentYear;
    }
}
