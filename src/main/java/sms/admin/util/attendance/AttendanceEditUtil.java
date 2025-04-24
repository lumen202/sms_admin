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

/**
 * Utility class for editing and managing attendance records in the Student
 * Management System.
 * Provides methods for creating UI components, updating attendance records, and
 * marking/unmarking holidays.
 */
public class AttendanceEditUtil {

    private static final Object ID_LOCK = new Object(); // Lock for synchronizing ID generation

    /**
     * Creates a ComboBox for selecting attendance status with predefined values.
     *
     * @param currentValue The current attendance status to set as the default
     *                     value.
     * @return A configured ComboBox with attendance status options.
     */
    public static ComboBox<String> createAttendanceComboBox(String currentValue) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(
                CommonAttendanceUtil.PRESENT_MARK,
                CommonAttendanceUtil.ABSENT_MARK,
                CommonAttendanceUtil.HALF_DAY_MARK,
                CommonAttendanceUtil.EXCUSED_MARK);
        comboBox.setValue(currentValue.isEmpty() ? CommonAttendanceUtil.PRESENT_MARK : currentValue);
        return comboBox;
    }

    /**
     * Handles the editing of an attendance record in a table cell, updating the
     * database and UI.
     *
     * @param cell           The table cell being edited.
     * @param student        The student associated with the attendance record.
     * @param date           The date of the attendance record.
     * @param attendanceLogs The list of attendance logs to update.
     * @param onComplete     Callback to handle the updated AttendanceLog.
     */
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

    /**
     * Updates an attendance record in the database for a specific student and date.
     *
     * @param student         The student whose attendance is being updated.
     * @param date            The date of the attendance record.
     * @param attendanceValue The new attendance status.
     * @param attendanceLogs  The list of attendance logs to update.
     * @return The updated or newly created AttendanceLog, or null if the operation
     *         fails.
     */
    private static AttendanceLog updateAttendanceRecord(
            Student student,
            LocalDate date,
            String attendanceValue,
            ObservableList<AttendanceLog> attendanceLogs) {
        try {
            return DatabaseRetryHelper.withRetry(() -> {
                synchronized (ID_LOCK) {
                    // Fetch fresh data from the database
                    List<AttendanceRecord> recordList = AttendanceRecordDAO.getRecordList();
                    List<AttendanceLog> dbLogs = AttendanceLogDAO.getAttendanceLogList();

                    System.out.println("Current records in DB: " + recordList.size());
                    System.out.println("Current logs in DB: " + (dbLogs != null ? dbLogs.size() : 0));

                    // Ensure a record exists for the date
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

                    // Check for an existing log
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

                    // Update the local collection on the JavaFX thread
                    if (resultLog != null) {
                        System.out.println("Operation successful, refreshing log list");
                        List<AttendanceLog> updatedLogs = AttendanceLogDAO.getAttendanceLogList();

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

    /**
     * Finds an existing attendance log for a student and record.
     *
     * @param student The student to search for.
     * @param record  The attendance record to search for.
     * @return The existing AttendanceLog, or null if not found or an error occurs.
     */
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

    /**
     * Updates the time values of an attendance log based on the provided status.
     *
     * @param log             The AttendanceLog to update.
     * @param attendanceValue The attendance status to apply.
     */
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

    /**
     * Ensures an attendance record exists for the specified date, creating a new
     * one if necessary.
     *
     * @param date The date to check or create a record for.
     * @return The existing or newly created AttendanceRecord.
     */
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

    /**
     * Marks a specific date as a holiday for all provided students.
     *
     * @param date                 The date to mark as a holiday.
     * @param students             The list of students to apply the holiday status
     *                             to.
     * @param masterAttendanceLogs The list of attendance logs to update.
     * @param onComplete           Callback to indicate operation success or
     *                             failure.
     */
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

    /**
     * Unmarks a specific date as a holiday, removing associated attendance records.
     *
     * @param date                 The date to unmark as a holiday.
     * @param masterAttendanceLogs The list of attendance logs to update.
     * @param onComplete           Callback to indicate operation success or
     *                             failure.
     */
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

    /**
     * Generates the next available log ID for a new attendance log.
     *
     * @return The next log ID, or -1 if generation fails.
     */
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