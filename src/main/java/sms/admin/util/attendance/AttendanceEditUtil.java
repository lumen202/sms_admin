package sms.admin.util.attendance;

import dev.finalproject.models.Student;
import dev.finalproject.models.AttendanceLog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import sms.admin.util.mock.DataUtil;

public class AttendanceEditUtil {
    
    public static ComboBox<String> createAttendanceComboBox(String currentValue) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(
            AttendanceUtil.PRESENT_MARK,
            AttendanceUtil.ABSENT_MARK,
            AttendanceUtil.HALF_DAY_MARK,
            AttendanceUtil.EXCUSED_MARK
        );
        comboBox.setValue(currentValue.isEmpty() ? AttendanceUtil.PRESENT_MARK : currentValue);
        return comboBox;
    }

    public static void handleAttendanceEdit(
            TableCell<Student, String> cell,
            Student student,
            LocalDate date,
            ObservableList<AttendanceLog> attendanceLog,
            Runnable onComplete) {
            
        String currentStatus = cell.getText();
        ComboBox<String> comboBox = createAttendanceComboBox(currentStatus);
        
        cell.setGraphic(comboBox);
        cell.setText(null);
        
        // Request focus after showing
        Platform.runLater(() -> {
            comboBox.requestFocus();
            comboBox.show();
        });

        // Handle focus lost
        comboBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                updateCellValue(cell, student, date, comboBox.getValue(), attendanceLog);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });

        // Handle selection
        comboBox.setOnAction(event -> {
            updateCellValue(cell, student, date, comboBox.getValue(), attendanceLog);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
    
    private static void updateCellValue(
            TableCell<Student, String> cell,
            Student student, 
            LocalDate date,
            String newValue,
            ObservableList<AttendanceLog> attendanceLog) {
            
        if (newValue != null) {
            if (newValue.equals(AttendanceUtil.EXCUSED_MARK)) {
                DataUtil.createExcusedAttendance(student, date);
            } else {
                updateAttendanceRecord(student, date, newValue, attendanceLog);
            }
            cell.setGraphic(null);
            cell.setText(newValue);
        }
    }
    
    private static void updateAttendanceRecord(
            Student student,
            LocalDate date,
            String attendanceValue,
            ObservableList<AttendanceLog> attendanceLog) {
            
        AttendanceLog log = AttendanceUtil.findOrCreateAttendanceLog(
            student,
            date,
            attendanceLog,
            DataUtil.createAttendanceRecordList()
        );
        
        switch (attendanceValue) {
            case AttendanceUtil.PRESENT_MARK:
                log.setTimeInAM(AttendanceUtil.TIME_IN_AM);
                log.setTimeOutAM(AttendanceUtil.TIME_OUT_AM);
                log.setTimeInPM(AttendanceUtil.TIME_IN_PM);
                log.setTimeOutPM(AttendanceUtil.TIME_OUT_PM);
                break;
            case AttendanceUtil.ABSENT_MARK:
                log.setTimeInAM(AttendanceUtil.TIME_ABSENT);
                log.setTimeOutAM(AttendanceUtil.TIME_ABSENT);
                log.setTimeInPM(AttendanceUtil.TIME_ABSENT);
                log.setTimeOutPM(AttendanceUtil.TIME_ABSENT);
                break;
            case AttendanceUtil.HALF_DAY_MARK:
                log.setTimeInAM(AttendanceUtil.TIME_IN_AM);
                log.setTimeOutAM(AttendanceUtil.TIME_OUT_AM);
                log.setTimeInPM(AttendanceUtil.TIME_ABSENT);
                log.setTimeOutPM(AttendanceUtil.TIME_ABSENT);
                break;
            case AttendanceUtil.EXCUSED_MARK:
                log.setTimeInAM(AttendanceUtil.TIME_EXCUSED);
                log.setTimeOutAM(AttendanceUtil.TIME_EXCUSED);
                log.setTimeInPM(AttendanceUtil.TIME_EXCUSED);
                log.setTimeOutPM(AttendanceUtil.TIME_EXCUSED);
                break;
        }
    }
}
