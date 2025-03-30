package sms.admin.util.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;

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
            Consumer<AttendanceLog> onComplete) {

        String currentStatus = cell.getText();
        ComboBox<String> comboBox = createAttendanceComboBox(currentStatus);

        cell.setGraphic(comboBox);
        cell.setText(null);

        comboBox.setOnAction(event -> {
            String newValue = comboBox.getValue();
            if (newValue != null) {
                Task<AttendanceLog> task = new Task<>() {
                    @Override
                    protected AttendanceLog call() {
                        return updateAttendanceRecord(student, date, newValue, attendanceLogs);
                    }
                };
                task.setOnSucceeded(e -> {
                    AttendanceLog log = task.getValue();
                    if (log != null) {
                        cell.setGraphic(null);
                        cell.setText(newValue);
                        if (onComplete != null) {
                            onComplete.accept(log);
                        }
                    } else {
                        cell.setGraphic(null);
                        cell.setText(AttendanceUtil.ABSENT_MARK);
                    }
                });
                task.setOnFailed(e -> {
                    System.err.println("Error updating attendance: " + task.getException().getMessage());
                    cell.setGraphic(null);
                    cell.setText(AttendanceUtil.ABSENT_MARK);
                });
                new Thread(task).start();
            }
        });

        Platform.runLater(() -> {
            comboBox.requestFocus();
            comboBox.show();
        });
    }

    private static AttendanceLog updateAttendanceRecord(
            Student student,
            LocalDate date,
            String attendanceValue,
            ObservableList<AttendanceLog> attendanceLogs) {
        AttendanceRecord record = ensureAttendanceRecordExists(date);

        AttendanceLog log = attendanceLogs.stream()
                .filter(l -> l.getStudentID().getStudentID() == student.getStudentID()
                && l.getRecordID().getRecordID() == record.getRecordID())
                .findFirst()
                .orElse(null);

        boolean isNewLog = (log == null);
        if (isNewLog) {
            log = new AttendanceLog(getNextLogId(), record, student, 0, 0, 0, 0);
        }

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

        if (isNewLog) {
            AttendanceLogDAO.insert(log);
        } else {
            AttendanceLogDAO.update(log);
        }
        return log;
    }

    private static AttendanceRecord ensureAttendanceRecordExists(LocalDate date) {
        List<AttendanceRecord> recordList = AttendanceRecordDAO.getRecordList();
        AttendanceRecord record = recordList.stream()
                .filter(r -> r.getYear() == date.getYear()
                && r.getMonth() == date.getMonthValue()
                && r.getDay() == date.getDayOfMonth())
                .findFirst()
                .orElse(null);
        if (record == null) {
            int nextId = recordList.stream()
                    .mapToInt(AttendanceRecord::getRecordID)
                    .max()
                    .orElse(0) + 1;
            record = new AttendanceRecord(nextId, date.getMonthValue(), date.getDayOfMonth(), date.getYear());
            AttendanceRecordDAO.insert(record);
        }
        return record;
    }

    private static int getNextLogId() {
        return AttendanceLogDAO.getAttendanceLogList().stream()
                .mapToInt(AttendanceLog::getLogID)
                .max()
                .orElse(0) + 1;
    }
}
