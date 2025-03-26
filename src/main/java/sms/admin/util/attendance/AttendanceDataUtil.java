package sms.admin.util.attendance;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AttendanceDataUtil {
    private static final Map<Integer, Student> studentCache = new ConcurrentHashMap<>();
    private static final Map<LocalDate, Map<Integer, AttendanceLog>> dateLogCache = new ConcurrentHashMap<>();
    private static final int BATCH_SIZE = 500;

    public static FilteredList<AttendanceLog> initializeAttendanceLogs(
            List<AttendanceLog> sourceLogs, 
            Set<Integer> studentIds) {
            
        // Process logs in batches
        List<AttendanceLog> validLogs = new ArrayList<>();
        for (int i = 0; i < sourceLogs.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, sourceLogs.size());
            List<AttendanceLog> batch = sourceLogs.subList(i, end);
            
            validLogs.addAll(batch.parallelStream()
                .filter(log -> isValidLog(log, studentIds))
                .collect(Collectors.toList()));
        }
            
        ObservableList<AttendanceLog> observableLogs = FXCollections.observableArrayList(validLogs);
        return new FilteredList<>(observableLogs, log -> !isLogInFuture(log));
    }
    
    public static Map<LocalDate, Map<Integer, AttendanceLog>> createDateToStudentLogsMap(
            List<AttendanceLog> logs) {
        if (logs == null) return new HashMap<>();

        return logs.parallelStream()
            .filter(log -> isValidLog(log, null))  // Pass null for studentIds when not needed
            .collect(Collectors.groupingBy(
                log -> LocalDate.of(
                    log.getRecordID().getYear(),
                    log.getRecordID().getMonth(),
                    log.getRecordID().getDay()),
                Collectors.toMap(
                    log -> log.getStudentID().getStudentID(),
                    log -> log,
                    (existing, replacement) -> replacement,
                    HashMap::new
                )
            ));
    }

    private static boolean isValidLog(AttendanceLog log, Set<Integer> studentIds) {
        if (studentIds == null) {
            return log != null && 
                   log.getRecordID() != null &&
                   log.getStudentID() != null;
        }
        return log != null && 
               log.getRecordID() != null &&
               log.getStudentID() != null && 
               studentIds.contains(log.getStudentID().getStudentID());
    }
    
    private static boolean isLogInFuture(AttendanceLog log) {
        if (log == null || log.getRecordID() == null) return true;
        
        LocalDate logDate = LocalDate.of(
            log.getRecordID().getYear(),
            log.getRecordID().getMonth(),
            log.getRecordID().getDay());
            
        return logDate.isAfter(LocalDate.now());
    }
    
    public static void updateAttendanceLogs(
            FilteredList<AttendanceLog> attendanceLog,
            Map<LocalDate, Map<Integer, AttendanceLog>> dateToStudentLogs,
            List<AttendanceLog> newLogs,
            Set<Integer> studentIds) {
            
        // Create new observable list and update
        List<AttendanceLog> validLogs = newLogs.stream()
            .filter(log -> isValidLog(log, studentIds))
            .collect(Collectors.toCollection(ArrayList::new));
            
        // Update the filtered list's source
        ((ObservableList<AttendanceLog>)attendanceLog.getSource()).setAll(validLogs);
        
        // Update the date map
        Map<LocalDate, Map<Integer, AttendanceLog>> newDateMap = createDateToStudentLogsMap(validLogs);
        dateToStudentLogs.clear();
        dateToStudentLogs.putAll(newDateMap);
    }

    public static void clearCaches() {
        studentCache.clear();
        dateLogCache.clear();
    }

    public static Student getCachedStudent(int studentId) {
        return studentCache.get(studentId);
    }

    public static void cacheStudent(Student student) {
        if (student != null) {
            studentCache.put(student.getStudentID(), student);
        }
    }

    public static Map<Integer, AttendanceLog> getCachedLogsForDate(LocalDate date) {
        return dateLogCache.getOrDefault(date, new HashMap<>());
    }
}
