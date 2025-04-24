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

import dev.finalproject.models.SchoolYear;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;

public class SchoolYearUtil {

    /**
     * Formats a {@link SchoolYear} as "YYYY-YYYY" (e.g., "2023-2024").
     *
     * @param schoolYear the SchoolYear object containing start and end years
     * @return formatted string representation of the school year
     */
    public static String formatSchoolYear(SchoolYear schoolYear) {
        return String.format("%d-%d",
                schoolYear.getYearStart(),
                schoolYear.getYearEnd());
    }

    /**
     * Converts an ObservableList of {@link SchoolYear} objects into an
     * ObservableList
     * of formatted strings for display (using
     * {@link #formatSchoolYear(SchoolYear)}).
     *
     * @param schoolYears the list of SchoolYear objects to convert
     * @return ObservableList of formatted school year strings
     */
    public static ObservableList<String> convertToStringList(
            ObservableList<SchoolYear> schoolYears) {
        return schoolYears.stream()
                .map(SchoolYearUtil::formatSchoolYear)
                .collect(FXCollections::observableArrayList,
                        ObservableList::add,
                        ObservableList::addAll);
    }

    /**
     * Checks if the given {@link SchoolYear} corresponds to the current academic
     * year.
     * <p>
     * Determines the current academic year based on the system date:
     * if the current month is before June (inclusive of January–May), the latter
     * part of the previous calendar year is considered; otherwise the current
     * calendar
     * year is used as the start of the academic year.
     * </p>
     *
     * @param sy the SchoolYear to check
     * @return true if sy represents the current academic year, false otherwise
     */
    public static boolean isCurrentYear(SchoolYear sy) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        // Academic year runs July–June; before June means we're still in the previous
        // year's cycle
        if (currentMonth < 6) {
            return sy.getYearStart() == (currentYear - 1);
        } else {
            return sy.getYearStart() == currentYear;
        }
    }

    /**
     * Finds and returns the {@link SchoolYear} in the provided list that matches
     * the
     * current academic year.
     *
     * @param schoolYears ObservableList of SchoolYear objects to search
     * @return the current SchoolYear, or null if none match
     */
    public static SchoolYear findCurrentYear(
            ObservableList<SchoolYear> schoolYears) {
        return schoolYears.stream()
                .filter(SchoolYearUtil::isCurrentYear)
                .findFirst()
                .orElse(null);
    }
}
