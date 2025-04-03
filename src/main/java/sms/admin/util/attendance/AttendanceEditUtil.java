package sms.admin.util.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.OptionalInt;

import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import sms.admin.util.db.DatabaseRetryHelper;

public class AttendanceEditUtil {

    public static ComboBox<String> createAttendanceComboBox(String currentValue) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(
                CommonAttendanceUtil.PRESENT_MARK,
                CommonAttendanceUtil.ABSENT_MARK,
                CommonAttendanceUtil.HALF_DAY_MARK,
                CommonAttendanceUtil.EXCUSED_MARK
        );
        comboBox.setValue(currentValue.isEmpty() ? CommonAttendanceUtil.PRESENT_MARK : currentValue);
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
                        cell.setText(CommonAttendanceUtil.ABSENT_MARK);
                    }
                });
                task.setOnFailed(e -> {
                    System.err.println("Error updating attendance: " + task.getException().getMessage());
                    cell.setGraphic(null);
                    cell.setText(CommonAttendanceUtil.ABSENT_MARK);
                });
                new Thread(task).start();
            }
        });

        Platform.runLater(() -> {
            comboBox.requestFocus();
            comboBox.show();
        });
    }

    private static final Object ID_LOCK = new Object();

    private static AttendanceLog updateAttendanceRecord(
            Student student,
            LocalDate date,
            String attendanceValue,
            ObservableList<AttendanceLog> attendanceLogs) {
        try {
            return DatabaseRetryHelper.withRetry(() -> {
                synchronized (ID_LOCK) {
                    // Get fresh data from database
                    List<AttendanceRecord> recordList = AttendanceRecordDAO.getRecordList();
                    List<AttendanceLog> dbLogs = AttendanceLogDAO.getAttendanceLogList();

                    System.out.println("Current records in DB: " + recordList.size());
                    System.out.println("Current logs in DB: " + (dbLogs != null ? dbLogs.size() : 0));

                    // Ensure record exists for the exact date
                    AttendanceRecord record = recordList.stream()
                            .filter(r -> r.getYear() == date.getYear()
                            && r.getMonth() == date.getMonthValue()
                            && r.getDay() == date.getDayOfMonth())
                            .findFirst()
                            .orElse(null);

                    if (record == null) {
                        int nextRecordId = recordList.stream()
                                .mapToInt(AttendanceRecord::getRecordID)
                                .max()
                                .orElse(0) + 1;
                        record = new AttendanceRecord(nextRecordId,
                                date.getMonthValue(),
                                date.getDayOfMonth(),
                                date.getYear());

                        System.out.printf("Creating new record for date: %s (ID: %d)%n",
                                date, nextRecordId);
                        AttendanceRecordDAO.insert(record);
                    }

                    final AttendanceRecord finalRecord = record;

                    // Get existing log for this exact record
                    AttendanceLog existingLog = dbLogs.stream()
                            .filter(l -> l.getStudentID().getStudentID() == student.getStudentID()
                            && l.getRecordID().getRecordID() == finalRecord.getRecordID())
                            .findFirst()
                            .orElse(null);

                    final AttendanceLog resultLog;
                    if (existingLog != null) {
                        System.out.printf("Updating existing log %d for date %s%n",
                                existingLog.getLogID(), date);
                        updateAttendanceValues(existingLog, attendanceValue);
                        AttendanceLogDAO.update(existingLog);
                        resultLog = existingLog;
                    } else {
                        int nextLogId = dbLogs.stream()
                                .mapToInt(AttendanceLog::getLogID)
                                .max()
                                .orElse(0) + 1;
                        AttendanceLog newLog = new AttendanceLog(nextLogId, finalRecord, student, 0, 0, 0, 0);
                        updateAttendanceValues(newLog, attendanceValue);

                        System.out.printf("Inserting new log %d for date %s%n",
                                nextLogId, date);
                        AttendanceLogDAO.insert(newLog);
                        resultLog = newLog;
                    }

                    // After successful insert/update
                    if (resultLog != null) {
                        System.out.println("Operation successful, refreshing log list");
                        // Get updated logs from database
                        List<AttendanceLog> updatedLogs = AttendanceLogDAO.getAttendanceLogList();

                        // Update local collection on FX thread
                        Platform.runLater(() -> {
                            attendanceLogs.clear();
                            if (updatedLogs != null) {
                                attendanceLogs.addAll(updatedLogs);
                            }
                            System.out.println("Local collection updated with " + attendanceLogs.size() + " logs");
                        });
                    }

                    return resultLog;
                }
            });
        } catch (Exception e) {
            System.err.println("Error in updateAttendanceRecord: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static AttendanceLog findExistingLog(Student student, AttendanceRecord record) {
        try {
            List<AttendanceLog> dbLogs = AttendanceLogDAO.getAttendanceLogList();
            return dbLogs.stream()
                    .filter(l -> l.getStudentID().getStudentID() == student.getStudentID()
                    && l.getRecordID().getRecordID() == record.getRecordID())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("Error finding existing log: " + e.getMessage());
            return null;
        }
    }

    private static void updateAttendanceValues(AttendanceLog log, String attendanceValue) {
        switch (attendanceValue) {
            case CommonAttendanceUtil.PRESENT_MARK -> {
                log.setTimeInAM(CommonAttendanceUtil.TIME_IN_AM);
                log.setTimeOutAM(CommonAttendanceUtil.TIME_OUT_AM);
                log.setTimeInPM(CommonAttendanceUtil.TIME_IN_PM);
                log.setTimeOutPM(CommonAttendanceUtil.TIME_OUT_PM);
            }
            case CommonAttendanceUtil.ABSENT_MARK -> {
                log.setTimeInAM(CommonAttendanceUtil.TIME_ABSENT);
                log.setTimeOutAM(CommonAttendanceUtil.TIME_ABSENT);
                log.setTimeInPM(CommonAttendanceUtil.TIME_ABSENT);
                log.setTimeOutPM(CommonAttendanceUtil.TIME_ABSENT);
            }
            case CommonAttendanceUtil.HALF_DAY_MARK -> {
                log.setTimeInAM(CommonAttendanceUtil.TIME_IN_AM);
                log.setTimeOutAM(CommonAttendanceUtil.TIME_OUT_AM);
                log.setTimeInPM(CommonAttendanceUtil.TIME_ABSENT);
                log.setTimeOutPM(CommonAttendanceUtil.TIME_ABSENT);
            }
            case CommonAttendanceUtil.EXCUSED_MARK -> {
                log.setTimeInAM(CommonAttendanceUtil.TIME_EXCUSED);
                log.setTimeOutAM(CommonAttendanceUtil.TIME_EXCUSED);
                log.setTimeInPM(CommonAttendanceUtil.TIME_EXCUSED);
                log.setTimeOutPM(CommonAttendanceUtil.TIME_EXCUSED);
            }
            case CommonAttendanceUtil.HOLIDAY_MARK -> {
                log.setTimeInAM(CommonAttendanceUtil.TIME_HOLIDAY);
                log.setTimeOutAM(CommonAttendanceUtil.TIME_HOLIDAY);
                log.setTimeInPM(CommonAttendanceUtil.TIME_HOLIDAY);
                log.setTimeOutPM(CommonAttendanceUtil.TIME_HOLIDAY);
            }
        }
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

    public static void markDayAsHoliday(LocalDate date, List<Student> students,
            ObservableList<AttendanceLog> masterAttendanceLogs, Consumer<Boolean> onComplete) {
        try {
            AttendanceRecord record = ensureAttendanceRecordExists(date);

            synchronized (ID_LOCK) {
                int nextId = generateNextLogId();

                for (Student student : students) {
                    AttendanceLog existingLog = findExistingLog(student, record);

                    if (existingLog != null) {
                        existingLog.setTimeInAM(CommonAttendanceUtil.TIME_HOLIDAY);
                        existingLog.setTimeOutAM(CommonAttendanceUtil.TIME_HOLIDAY);
                        existingLog.setTimeInPM(CommonAttendanceUtil.TIME_HOLIDAY);
                        existingLog.setTimeOutPM(CommonAttendanceUtil.TIME_HOLIDAY);
                        AttendanceLogDAO.update(existingLog);
                        if (masterAttendanceLogs.contains(existingLog)) {
                            masterAttendanceLogs.set(masterAttendanceLogs.indexOf(existingLog), existingLog);
                        } else {
                            masterAttendanceLogs.add(existingLog);
                        }
                    } else {
                        AttendanceLog newLog = new AttendanceLog(nextId++, record, student,
                                CommonAttendanceUtil.TIME_HOLIDAY,
                                CommonAttendanceUtil.TIME_HOLIDAY,
                                CommonAttendanceUtil.TIME_HOLIDAY,
                                CommonAttendanceUtil.TIME_HOLIDAY);
                        AttendanceLogDAO.insert(newLog);
                        masterAttendanceLogs.add(newLog);
                    }
                }
            }

            CommonAttendanceUtil.addHolidayDate(date);
            onComplete.accept(true);
        } catch (Exception e) {
            System.err.println("Error marking holiday: " + e.getMessage());
            e.printStackTrace();
            onComplete.accept(false);
        }
    }

    public static void unmarkDayAsHoliday(LocalDate date,
            ObservableList<AttendanceLog> masterAttendanceLogs, Consumer<Boolean> onComplete) {
        try {
            List<AttendanceRecord> recordList = AttendanceRecordDAO.getRecordList();
            AttendanceRecord record = recordList.stream()
                    .filter(r -> r.getYear() == date.getYear()
                    && r.getMonth() == date.getMonthValue()
                    && r.getDay() == date.getDayOfMonth())
                    .findFirst()
                    .orElse(null);

            if (record != null) {
                AttendanceRecordDAO.delete(record);
                masterAttendanceLogs.removeIf(log -> log.getRecordID().getRecordID() == record.getRecordID());
            }

            CommonAttendanceUtil.removeHolidayDate(date);
            onComplete.accept(true);
        } catch (Exception e) {
            System.err.println("Error unmarking holiday: " + e.getMessage());
            e.printStackTrace();
            onComplete.accept(false);
        }
    }

    private static synchronized int generateNextLogId() {
        try {
            return DatabaseRetryHelper.withRetry(() -> {
                List<AttendanceLog> allLogs = AttendanceLogDAO.getAttendanceLogList();
                int maxId = allLogs.stream()
                        .mapToInt(AttendanceLog::getLogID)
                        .max()
                        .orElse(0);
                return maxId + 1;
            });
        } catch (Exception e) {
            System.err.println("Failed to generate next log ID: " + e.getMessage());
            return -1;
        }
    }
}
