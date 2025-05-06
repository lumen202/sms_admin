package sms.admin.util.datetime;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.time.Month;

/**
 * Utility class for generating and retrieving academic year-related data.
 * Academic years are formatted as "YYYY-YYYY" (e.g., "2024-2025").
 * 
 * The academic year is assumed to start in June and end in May of the following
 * year.
 */
public class YearData {

    /**
     * Generates a list of upcoming academic years starting from the current one.
     * 
     * @return an {@link ObservableList} of academic year strings in the format
     *         "YYYY-YYYY"
     */
    public static ObservableList<String> getYears() {
        ObservableList<String> years = FXCollections.observableArrayList();
        int academicYear = getCurrentStartYear();

        // Generate 5 consecutive academic years starting from the current one
        for (int i = 0; i < 5; i++) {
            years.add((academicYear + i) + "-" + (academicYear + i + 1));
        }
        return years;
    }

    /**
     * Retrieves the current academic year based on today's date.
     * 
     * @return the current academic year in the format "YYYY-YYYY"
     */
    public static String getCurrentAcademicYear() {
        int startYear = getCurrentStartYear();
        return startYear + "-" + (startYear + 1);
    }

    /**
     * Determines the start year of the current academic year.
     * If the current month is before June, the academic year started the previous
     * year.
     * 
     * @return the starting year of the current academic year
     */
    private static int getCurrentStartYear() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        // Academic year starts in June
        if (today.getMonth().getValue() < Month.JUNE.getValue()) {
            return currentYear - 1;
        }
        return currentYear;
    }
}
