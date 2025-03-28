package sms.admin.util.attendance;

import dev.finalproject.models.Student;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
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
            ObservableList<AttendanceLog> attendanceLogs,
            Runnable onComplete) {
            
        String currentStatus = cell.getText();
        ComboBox<String> comboBox = createAttendanceComboBox(currentStatus);
        
        cell.setGraphic(comboBox);
        cell.setText(null);
        
        Platform.runLater(() -> {
            comboBox.requestFocus();
            comboBox.show();
        });

        comboBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                updateCellValue(cell, student, date, comboBox.getValue(), attendanceLogs);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });

        comboBox.setOnAction(event -> {
            updateCellValue(cell, student, date, comboBox.getValue(), attendanceLogs);
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
            ObservableList<AttendanceLog> attendanceLogs) {
            
        if (newValue != null) {
            try {
                AttendanceLog log;
                if (newValue.equals(AttendanceUtil.EXCUSED_MARK)) {
                    log = DataUtil.createExcusedAttendance(student, date);
                } else {
                    log = updateAttendanceRecord(student, date, newValue, attendanceLogs);
                }
                cell.setGraphic(null);
                cell.setText(newValue);
            } catch (Exception e) {
                System.err.println("Error updating attendance: " + e.getMessage());
                cell.setGraphic(null);
                cell.setText(AttendanceUtil.ABSENT_MARK);
            }
        }
    }
    
    private static AttendanceLog updateAttendanceRecord(
            Student student,
            LocalDate date,
            String attendanceValue,
            ObservableList<AttendanceLog> attendanceLogs) {
            
        // Get or create the AttendanceRecord
        AttendanceRecord record = getOrCreateRecord(date);
        
        // Find existing log or create a new one
        AttendanceLog log = attendanceLogs.stream()
            .filter(l -> l.getStudentID().getStudentID() == student.getStudentID() &&
                         l.getRecordID().getRecordID() == record.getRecordID())
            .findFirst()
            .orElseGet(() -> {
                int nextLogId = getNextLogId(attendanceLogs);
                return new AttendanceLog(nextLogId, record, student, 0, 0, 0, 0);
            });
        
        // Update log times
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

        // Persist to database
        if (attendanceLogs.contains(log)) {
            AttendanceLogDAO.update(log);
        } else {
            AttendanceLogDAO.insert(log);
            attendanceLogs.add(log);
        }

        return log;
    }

    private static AttendanceRecord getOrCreateRecord(LocalDate date) {
        AttendanceRecord record = AttendanceRecordDAO.getRecordList().stream()
            .filter(r -> r.getYear() == date.getYear() && 
                        r.getMonth() == date.getMonthValue() && 
                        r.getDay() == date.getDayOfMonth())
            .findFirst()
            .orElse(null);
        if (record == null) {
            int nextId = AttendanceRecordDAO.getRecordList().stream()
                .mapToInt(AttendanceRecord::getRecordID)
                .max()
                .orElse(0) + 1;
            record = new AttendanceRecord(nextId, date.getMonthValue(), date.getDayOfMonth(), date.getYear());
            AttendanceRecordDAO.insert(record);
        }
        return record;
    }

    private static int getNextLogId(ObservableList<AttendanceLog> attendanceLogs) {
        return Math.max(
            AttendanceLogDAO.getAttendanceLogList().stream().mapToInt(AttendanceLog::getLogID).max().orElse(0),
            attendanceLogs.stream().mapToInt(AttendanceLog::getLogID).max().orElse(0)
        ) + 1;
    }
}