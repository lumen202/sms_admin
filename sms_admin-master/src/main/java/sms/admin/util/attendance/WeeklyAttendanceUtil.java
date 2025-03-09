package sms.admin.util.attendance;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;

public class WeeklyAttendanceUtil {
    private static final String WEEK_RANGE_SEPARATOR = " - ";
    private static final double DEFAULT_DAY_COLUMN_WIDTH = 120.0;
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd");

    public static LocalDate getFirstDayOfMonth(String selectedMonthYear) {
        String[] parts = selectedMonthYear.split(" ");
        String monthName = parts[0];
        int yearNumber = Integer.parseInt(parts[1]);
        Month month = Month.valueOf(monthName.toUpperCase());
        LocalDate firstDay = LocalDate.of(yearNumber, month.getValue(), 1);
        
        // Skip to first weekday
        while (AttendanceUtil.isWeekend(firstDay)) {
            firstDay = firstDay.plusDays(1);
        }
        return firstDay;
    }

    public static LocalDate findWeekEndDate(LocalDate start, int month) {
        LocalDate current = start;
        while (current.getMonthValue() == month) {
            if (current.getDayOfWeek().getValue() == 5 || 
                current.plusDays(1).getMonthValue() != month) {
                return current;
            }
            current = current.plusDays(1);
        }
        return null;
    }

    public static TableColumn<Student, String> createDayColumn(
            LocalDate date,
            ObservableList<AttendanceLog> attendanceLogs) {
            
        // More compact format: {day}{initial}
        String columnHeader = String.format("%d%s", 
            date.getDayOfMonth(),
            getDayInitial(date.getDayOfWeek().getValue())
        );
        
        TableColumn<Student, String> dayColumn = new TableColumn<>(columnHeader);
        dayColumn.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            String attendanceStatus = AttendanceUtil.getAttendanceStatus(student, date, attendanceLogs);
            return new SimpleStringProperty(attendanceStatus);
        });
        
        dayColumn.setStyle("-fx-alignment: CENTER; -fx-font-size: 11px;"); // Smaller font
        return dayColumn;
    }
    
    private static String getDayInitial(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1: return "M";
            case 2: return "T";
            case 3: return "W";
            case 4: return "Th";
            case 5: return "F";
            case 6: return "Sa";
            case 7: return "Su";
            default: return "";
        }
    }

    public static void updateWeeklyColumns(
            ObservableList<TableColumn<Student, ?>> weekColumns,
            String selectedMonthYear,
            String selectedWeek,
            ObservableList<AttendanceLog> attendanceLogs) {
        
        if (selectedMonthYear == null || selectedWeek == null || weekColumns == null) {
            return;
        }

        String[] parts = selectedMonthYear.split(" ");
        if (parts.length < 2) return;
        
        String monthName = parts[0];
        int yearNumber = Integer.parseInt(parts[1]);
        Month month = Month.valueOf(monthName.toUpperCase());

        String[] weekRange = selectedWeek.split(WEEK_RANGE_SEPARATOR);
        if (weekRange.length < 2) return;
        
        int startDay = Integer.parseInt(weekRange[0]);
        int endDay = Integer.parseInt(weekRange[1]);
        
        LocalDate date = LocalDate.of(yearNumber, month.getValue(), startDay);
        LocalDate endDate = LocalDate.of(yearNumber, month.getValue(), endDay);

        weekColumns.clear();
        while (!date.isAfter(endDate)) {
            if (!AttendanceUtil.isWeekend(date)) {
                TableColumn<Student, String> dayColumn = createDayColumn(date, attendanceLogs);
                if (dayColumn != null) {
                    weekColumns.add(dayColumn);
                }
            }
            date = date.plusDays(1);
        }
    }

    public static void populateWeekComboBox(ComboBox<String> weekComboBox, LocalDate firstDayOfMonth) {
        LocalDate currentDay = firstDayOfMonth;
        int currentMonth = firstDayOfMonth.getMonthValue();

        while (currentDay.getMonthValue() == currentMonth) {
            LocalDate weekEnd = findWeekEndDate(currentDay, currentMonth);
            if (weekEnd != null) {
                String weekRange = currentDay.format(DAY_FORMATTER) +
                    WEEK_RANGE_SEPARATOR +
                    weekEnd.format(DAY_FORMATTER);
                weekComboBox.getItems().add(weekRange);
                
                currentDay = weekEnd.plusDays(1);
                while (currentDay.getMonthValue() == currentMonth && 
                       AttendanceUtil.isWeekend(currentDay)) {
                    currentDay = currentDay.plusDays(1);
                }
            } else {
                break;
            }
        }
    }

    public static void setDefaultWeek(ComboBox<String> weekComboBox, int monthNumber, int yearNumber) {
        if (weekComboBox.getItems().isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();

        // Only set current week if we're in the current month
        if (monthNumber == today.getMonthValue() && yearNumber == today.getYear()) {
            // Find the week that contains today
            for (String weekRange : weekComboBox.getItems()) {
                String[] range = weekRange.split(WEEK_RANGE_SEPARATOR);
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);

                LocalDate weekStart = LocalDate.of(yearNumber, monthNumber, start);
                LocalDate weekEnd = LocalDate.of(yearNumber, monthNumber, end);

                if (!today.isBefore(weekStart) && !today.isAfter(weekEnd)) {
                    weekComboBox.setValue(weekRange);
                    return;
                }
            }
        }

        // If not current month or week not found, select first week
        weekComboBox.setValue(weekComboBox.getItems().get(0));
    }
}