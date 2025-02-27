package sms.admin.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class MonthYearUtil {

    /**
     * Populates the provided ComboBox with month–year combinations for the
     * specified year.
     * If the given year is the current year, the default value is set to the
     * current month;
     * otherwise, the first month is selected.
     *
     * @param comboBox     the ComboBox to update
     * @param selectedYear the year as a String (e.g. "2023")
     */
    public static void updateMonthYearComboBox(ComboBox<String> comboBox, String selectedYear) {
        ObservableList<String> monthYearList = FXCollections.observableArrayList();
        String[] months = { "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December" };
        for (String month : months) {
            monthYearList.add(month + " " + selectedYear);
        }
        comboBox.setItems(monthYearList);

        LocalDate now = LocalDate.now();
        String defaultMonthYear;
        if (String.valueOf(now.getYear()).equals(selectedYear)) {
            defaultMonthYear = now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + now.getYear();
        } else {
            defaultMonthYear = monthYearList.get(0);
        }
        comboBox.setValue(defaultMonthYear);
    }
}
