/**
 * Utility class for formatting and handling {@link dev.finalproject.models.SchoolYear} instances
 * within the SMS administrative module.
 * <p>
 * Provides methods to convert SchoolYear objects to display-friendly strings,
 * identify the current academic year based on the system date, and select
 * the matching SchoolYear from a list.
 * </p>
 */
package sms.admin.util.datetime;

import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import dev.finalproject.models.SchoolYear;

public class SchoolYearUtil {

    /**
     * Formats a SchoolYear object into a string representation (e.g., "2023-2024").
     * 
     * @param schoolYear The SchoolYear object to format
     * @return String in "YYYY-YYYY" format
     */
    public static String formatSchoolYear(SchoolYear schoolYear) {
        if (schoolYear == null)
            return "";
        return String.format("%d-%d", schoolYear.getYearStart(), schoolYear.getYearEnd());
    }

    /**
     * Finds the current school year from a list of school years.
     * 
     * @param schoolYears The list of SchoolYear objects to search
     * @return The current SchoolYear, or null if not found
     */
    public static SchoolYear findCurrentYear(ObservableList<SchoolYear> schoolYears) {
        if (schoolYears == null || schoolYears.isEmpty()) {
            return null;
        }

        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        // If we're in the later months of the year (July-December), look for school
        // year starting this year
        // Otherwise look for school year that started in the previous year
        int targetStartYear = currentMonth >= 7 ? currentYear : currentYear - 1;

        return schoolYears.stream()
                .filter(year -> year.getYearStart() == targetStartYear)
                .findFirst()
                .orElse(schoolYears.get(0)); // Default to first year if no match found
    }

    /**
     * Converts an ObservableList of SchoolYear objects into an ObservableList
     * of formatted strings for display.
     *
     * @param schoolYears the list of SchoolYear objects to convert
     * @return ObservableList of formatted school year strings
     */
    public static ObservableList<String> convertToStringList(ObservableList<SchoolYear> schoolYears) {
        return schoolYears.stream()
                .map(SchoolYearUtil::formatSchoolYear)
                .collect(FXCollections::observableArrayList,
                        ObservableList::add,
                        ObservableList::addAll);
    }
}
