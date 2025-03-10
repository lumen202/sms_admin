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

public class DataUtil {

    // Observable lists to hold the data
    private static final ObservableList<Student> studentList = FXCollections.observableArrayList();
    private static final ObservableList<Guardian> guardianList = FXCollections.observableArrayList();
    private static final ObservableList<Cluster> clusterList = FXCollections.observableArrayList();
    private static final ObservableList<SchoolYear> schoolYearList = FXCollections.observableArrayList();
    private static final ObservableList<Address> addressList = FXCollections.observableArrayList();
    private static final ObservableList<AttendanceRecord> attendanceRecordList = FXCollections.observableArrayList();
    private static final ObservableList<AttendanceLog> attendanceLogList = FXCollections.observableArrayList();
    private static final ObservableList<StudentGuardian> studentGuardianList = FXCollections.observableArrayList();

    // Method to create an ObservableList of Student
    public static ObservableList<Student> createStudentList() {
        if (studentList.isEmpty()) {
            SchoolYear year2023 = new SchoolYear(1, 2023, 2024, "August", "May", 1, 31);
            SchoolYear year2024 = new SchoolYear(2, 2024, 2025, "September", "June", 1, 30);

            schoolYearList.clear();
            schoolYearList.addAll(year2023, year2024);

            studentList.add(new Student(1, "John", "A.", "Doe", "Jr.", "john.doe@example.com", "Active", "1234567890", new Date(), 100.0, new Cluster(1, "Science"), year2023));
            studentList.add(new Student(2, "Jane", "B.", "Smith", "", "jane.smith@example.com", "Active", "0987654321", new Date(), 150.0, new Cluster(2, "Arts"), year2023));
            studentList.add(new Student(3, "Mike", "C.", "Johnson", "", "mike.j@example.com", "Active", "5555555555", new Date(), 120.0, new Cluster(1, "Science"), year2024));
            studentList.add(new Student(4, "Sarah", "D.", "Williams", "", "sarah.w@example.com", "Active", "6666666666", new Date(), 130.0, new Cluster(2, "Arts"), year2024));
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

    // Method to create an ObservableList of AttendanceRecord (Weekdays only)
    public static ObservableList<AttendanceRecord> createAttendanceRecordList() {
        if (attendanceRecordList.isEmpty()) {
            // Records for 2023-2024 (March 4-8, 2024 - Monday to Friday)
            
            attendanceRecordList.add(new AttendanceRecord(1, 3, 4, 2024));
            attendanceRecordList.add(new AttendanceRecord(2, 3, 5, 2024));
            attendanceRecordList.add(new AttendanceRecord(3, 3, 6, 2024));
            attendanceRecordList.add(new AttendanceRecord(4, 3, 7, 2024));
            attendanceRecordList.add(new AttendanceRecord(5, 3, 8, 2024));

            // Records for 2024-2025 (March 3-7, 2025 - Monday to Friday)
            attendanceRecordList.add(new AttendanceRecord(6, 3, 3, 2025));
            attendanceRecordList.add(new AttendanceRecord(7, 3, 4, 2025));
            attendanceRecordList.add(new AttendanceRecord(8, 3, 5, 2025));
            attendanceRecordList.add(new AttendanceRecord(9, 3, 6, 2025));
            attendanceRecordList.add(new AttendanceRecord(10, 3, 7, 2025));
        }
        return attendanceRecordList;
    }

    // Method to create an ObservableList of AttendanceLog with excused value
    public static ObservableList<AttendanceLog> createAttendanceLogList() {
        if (attendanceLogList.isEmpty()) {
            ObservableList<Student> students = createStudentList();
            ObservableList<AttendanceRecord> records = createAttendanceRecordList();

            int[][] times = {
                {800, 1200, 1300, 1500}, // Normal day
                {900, 1200, 1300, 1500}, // Late arrival
                {800, 1100, 1300, 1500}, // Early departure AM
                {800, 1200, 1300, 1400}, // Early departure PM
                {3000, 3000, 3000, 3000} // Excused absence (replaces half day)
            };

            int logId = 1;

            // Logs for 2023-2024 students (John and Jane) for March 2024
            ObservableList<Student> year2023Students = FXCollections.observableArrayList(
                    students.get(0), // John
                    students.get(1) // Jane
            );

            for (Student student : year2023Students) {
                for (int i = 0; i < 5; i++) {  // First 5 records are for 2024
                    int[] timeSet = times[i % times.length];
                    attendanceLogList.add(new AttendanceLog(
                            logId++,
                            records.get(i),
                            student,
                            timeSet[0], // timeInAM
                            timeSet[1], // timeOutAM
                            timeSet[2], // timeInPM
                            timeSet[3] // timeOutPM
                    ));
                }
            }

            // Logs for 2024-2025 students (Mike and Sarah) for March 2025
            ObservableList<Student> year2024Students = FXCollections.observableArrayList(
                    students.get(2), // Mike
                    students.get(3) // Sarah
            );

            for (Student student : year2024Students) {
                for (int i = 5; i < 10; i++) {  // Last 5 records are for 2025
                    int[] timeSet = times[(i - 5) % times.length];  // Offset index for 2025 records
                    attendanceLogList.add(new AttendanceLog(
                            logId++,
                            records.get(i),
                            student,
                            timeSet[0], // timeInAM
                            timeSet[1], // timeOutAM
                            timeSet[2], // timeInPM
                            timeSet[3] // timeOutPM
                    ));
                }
            }
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
        AttendanceLog existingLog = attendanceLogList.stream()
                .filter(log -> log.getStudentID().equals(student)
                && log.getRecordID().getDay() == date.getDayOfMonth()
                && log.getRecordID().getMonth() == date.getMonthValue()
                && log.getRecordID().getYear() == date.getYear())
                .findFirst()
                .orElse(null);

        if (existingLog != null) {
            existingLog.setTimeInAM(3000);
            existingLog.setTimeOutAM(3000);
            existingLog.setTimeInPM(3000);
            existingLog.setTimeOutPM(3000);
            return existingLog;
        }

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
                3000, // timeInAM
                3000, // timeOutAM
                3000, // timeInPM
                3000 // timeOutPM
        );

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
                .filter(log -> log.getStudentID().equals(student)
                && log.getRecordID().getDay() == date.getDayOfMonth()
                && log.getRecordID().getMonth() == date.getMonthValue()
                && log.getRecordID().getYear() == date.getYear())
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
