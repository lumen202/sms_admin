package sms.admin.util.mock;

import java.util.Date;
import java.time.LocalDate;

import dev.finalproject.models.Address;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Cluster;
import dev.finalproject.models.Guardian;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
import dev.finalproject.models.StudentGuardian;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sms.admin.util.attendance.AttendanceUtil;

public class DataUtil {

    // Observable lists to hold the data
    private static final ObservableList<Student> studentList = FXCollections.observableArrayList();
    private static final ObservableList<Guardian> guardianList = FXCollections.observableArrayList();
    private static final  ObservableList<Cluster> clusterList = FXCollections.observableArrayList();
    private static final ObservableList<SchoolYear> schoolYearList = FXCollections.observableArrayList();
    private static final ObservableList<Address> addressList = FXCollections.observableArrayList();
    private static final ObservableList<AttendanceRecord> attendanceRecordList = FXCollections.observableArrayList();
    private static final ObservableList<AttendanceLog> attendanceLogList = FXCollections.observableArrayList();
    private static final ObservableList<StudentGuardian> studentGuardianList = FXCollections.observableArrayList();

    // Method to create an ObservableList of Student
    public static ObservableList<Student> createStudentList() {
        if (studentList.isEmpty()) {
            studentList.add(new Student(1, "John", "A.", "Doe", "Jr.", "john.doe@example.com", "Active", "1234567890", new Date(), 100.0, new Cluster(1, "Science"), new SchoolYear(1, 2023, 2024, "August", "May", 1, 31)));
            studentList.add(new Student(2, "Jane", "B.", "Smith", "", "jane.smith@example.com", "Active", "0987654321", new Date(), 150.0, new Cluster(2, "Arts"), new SchoolYear(2, 2024, 2025, "September", "June", 1, 30)));
        }
        return studentList;
    }

    // Method to create an ObservableList of Guardian
    public static ObservableList<Guardian> createGuardianList() {
        if (guardianList.isEmpty()) {
            guardianList.add(new Guardian(1, "Alice", "B.", "Johnson", "Mother", "123-456-7890"));
            guardianList.add(new Guardian(2, "Bob", "C.", "Smith", "Father", "098-765-4321"));
        }
        return guardianList;
    }

    // Method to create an ObservableList of StudentGuardian
    public static ObservableList<StudentGuardian> createStudentGuardianList() {
        if (studentGuardianList.isEmpty()) {
            Student student1 = createStudentList().get(0); // John Doe
            Guardian guardian1 = createGuardianList().get(0); // Alice Johnson
            studentGuardianList.add(new StudentGuardian(student1, guardian1));
        }
        return studentGuardianList;
    }

    // Method to create an ObservableList of Address
    public static ObservableList<Address> createAddressList() {
        if (addressList.isEmpty()) {
            Student student1 = createStudentList().get(0); // John Doe
            addressList.add(new Address(student1, 1, "New York", "Manhattan", "5th Avenue", "Central Park", 10001));
            addressList.add(new Address(student1, 2, "Los Angeles", "LA", "Sunset Boulevard", "Hollywood", 90001));
        }
        return addressList;
    }

    // Method to create an ObservableList of AttendanceRecord
    public static ObservableList<AttendanceRecord> createAttendanceRecordList() {
        if (attendanceRecordList.isEmpty()) {
            attendanceRecordList.add(new AttendanceRecord(1, 3, 7, 2025));
            attendanceRecordList.add(new AttendanceRecord(2, 3, 6, 2025));
        }
        return attendanceRecordList;
    }

    // Method to create an ObservableList of AttendanceLog
    public static ObservableList<AttendanceLog> createAttendanceLogList() {
        if (attendanceLogList.isEmpty()) {
            ObservableList<Student> students = createStudentList();
            ObservableList<AttendanceRecord> records = createAttendanceRecordList();
            attendanceLogList.add(new AttendanceLog(1, records.get(0), students.get(0), 0, 0, 1700, 2000));
            attendanceLogList.add(new AttendanceLog(2, records.get(1), students.get(0), 900, 1300, 1800, 2000));
        }
        return attendanceLogList;
    }

    // Method to create an ObservableList of Cluster
    public static ObservableList<Cluster> createClusterList() {
        if (clusterList.isEmpty()) {
            clusterList.add(new Cluster(1, "Science"));
            clusterList.add(new Cluster(2, "Arts"));
            clusterList.add(new Cluster(3, "Commerce"));
        }
        return clusterList;
    }

    // Method to create an ObservableList of SchoolYear
    public static ObservableList<SchoolYear> createSchoolYearList() {
        if (schoolYearList.isEmpty()) {
            schoolYearList.add(new SchoolYear(1, 2023, 2024, "August", "May", 1, 31));
            schoolYearList.add(new SchoolYear(2, 2024, 2025, "September", "June", 1, 30));
        }
        return schoolYearList;
    }

    public static AttendanceLog createExcusedAttendance(Student student, LocalDate date) {
        // First check if log already exists
        AttendanceLog existingLog = attendanceLogList.stream()
            .filter(log -> log.getStudentID().equals(student) && 
                        log.getRecordID().getDay() == date.getDayOfMonth() &&
                        log.getRecordID().getMonth() == date.getMonthValue() &&
                        log.getRecordID().getYear() == date.getYear())
            .findFirst()
            .orElse(null);
        
        if (existingLog != null) {
            // Update existing log
            existingLog.setTimeInAM(AttendanceUtil.TIME_EXCUSED);
            existingLog.setTimeOutAM(AttendanceUtil.TIME_EXCUSED);
            existingLog.setTimeInPM(AttendanceUtil.TIME_EXCUSED);
            existingLog.setTimeOutPM(AttendanceUtil.TIME_EXCUSED);
            return existingLog;
        }
        
        // Create new record and log if none exists
        AttendanceRecord record = new AttendanceRecord(
            generateNextId(),
            date.getMonthValue(),
            date.getDayOfMonth(),
            date.getYear()
        );
        
        AttendanceLog log = new AttendanceLog(
            generateNextId(),
            record,
            student,
            AttendanceUtil.TIME_EXCUSED,
            AttendanceUtil.TIME_EXCUSED,
            AttendanceUtil.TIME_EXCUSED,
            AttendanceUtil.TIME_EXCUSED
        );
        
        // Add to lists
        attendanceRecordList.add(record);
        attendanceLogList.add(log);
        
        return log;
    }

    private static int generateNextId() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }

    public static boolean updateAttendanceStatus(
            Student student, 
            LocalDate date, 
            int timeInAM, 
            int timeOutAM, 
            int timeInPM, 
            int timeOutPM) {
            
        AttendanceLog existingLog = attendanceLogList.stream()
            .filter(log -> log.getStudentID().equals(student) && 
                         log.getRecordID().getDay() == date.getDayOfMonth() &&
                         log.getRecordID().getMonth() == date.getMonthValue() &&
                         log.getRecordID().getYear() == date.getYear())
            .findFirst()
            .orElse(null);
            
        if (existingLog != null) {
            existingLog.setTimeInAM(timeInAM);
            existingLog.setTimeOutAM(timeOutAM);
            existingLog.setTimeInPM(timeInPM);
            existingLog.setTimeOutPM(timeOutPM);
            return true;
        }
        
        return false;
    }
}