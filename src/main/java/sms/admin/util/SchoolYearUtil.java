package sms.admin.util;

import dev.finalproject.models.SchoolYear;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;

public class SchoolYearUtil {
    
    /**
     * Format school year as "YYYY-YYYY" (e.g., "2023-2024")
     */
    public static String formatSchoolYear(SchoolYear schoolYear) {
        return String.format("%d-%d", 
            schoolYear.getYearStart(), 
            schoolYear.getYearEnd());
    }

    /**
     * Convert list of SchoolYear objects to list of formatted strings for display
     */
    public static ObservableList<String> convertToStringList(ObservableList<SchoolYear> schoolYears) {
        return schoolYears.stream()
            .<String>map(SchoolYearUtil::formatSchoolYear)
            .collect(FXCollections::observableArrayList, 
                    ObservableList::add, 
                    ObservableList::addAll);
    }

    /**
     * Check if the given school year is the current academic year
     */
    public static boolean isCurrentYear(SchoolYear sy) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        
        // If current month is before June, we're in the latter part of the school year
        if (currentMonth < 6) {
            return sy.getYearStart() == currentYear - 1;
        } else {
            return sy.getYearStart() == currentYear;
        }
    }

    /**
     * Find the current school year from a list of school years
     */
    public static SchoolYear findCurrentYear(ObservableList<SchoolYear> schoolYears) {
        return schoolYears.stream()
            .filter(SchoolYearUtil::isCurrentYear)
            .findFirst()
            .orElse(null);
    }
}
