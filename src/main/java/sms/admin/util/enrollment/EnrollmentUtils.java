package sms.admin.util.enrollment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dev.finalproject.data.AddressDAO;
import dev.finalproject.data.ClusterDAO;
import dev.finalproject.data.StudentDAO;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.Address;
import dev.finalproject.models.Cluster;
import dev.finalproject.models.Guardian;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;

public class EnrollmentUtils {

    // Constants matching DB schema column sizes
    private static final int MAX_NAME_LENGTH = 20;
    private static final int MAX_EMAIL_LENGTH = 30;
    private static final int MAX_CONTACT_LENGTH = 15;
    private static final int MAX_STATUS_LENGTH = 20;
    private static final int MAX_STREET_LENGTH = 50;
    private static final int MAX_CITY_LENGTH = 50;
    private static final int MAX_MUNICIPALITY_LENGTH = 50;
    private static final int MAX_BARANGAY_LENGTH = 50;

    // For manual ID generation since DAO does not support auto-increment.
    private static final Object STUDENT_ID_LOCK = new Object();
    private static int lastGeneratedStudentId = -1;

    /**
     * Enrolls a student by creating a new cluster (using the provided
     * clusterName if available), inserting the student record, and then
     * creating an address record.
     *
     * Now, if a valid studentId (nonzero) is passed in, it will be used.
     */
    public static Student enrollStudent(
            String studentId, // if provided and > 0, it will be used.
            String firstName, String middleName, String lastName, String nameExt,
            String email, String status, String contact, Date dateOfBirth,
            double fare, String street, String barangay, String city, String municipality,
            String postalCode, Guardian guardian, String clusterName, SchoolYear schoolYear) throws Exception {

        // Use provided studentId if valid; otherwise generate one.
        int studentIdInt;
        try {
            studentIdInt = Integer.parseInt(studentId);
            if (studentIdInt <= 0) {  // treat 0 as invalid
                throw new NumberFormatException();
            }
        } catch (Exception e) {
            studentIdInt = generateNextStudentId();
        }

        // Truncate and sanitize fields.
        firstName = truncateString(firstName, MAX_NAME_LENGTH);
        middleName = truncateString(middleName, MAX_NAME_LENGTH);
        lastName = truncateString(lastName, MAX_NAME_LENGTH);
        nameExt = truncateString(nameExt, 10);
        email = truncateString(email, MAX_EMAIL_LENGTH);
        status = truncateString(status, MAX_STATUS_LENGTH);
        contact = truncateString(contact, MAX_CONTACT_LENGTH);

        street = sanitizeAddressField(street, MAX_STREET_LENGTH);
        barangay = sanitizeAddressField(barangay, MAX_BARANGAY_LENGTH);
        city = sanitizeAddressField(city, MAX_CITY_LENGTH);
        municipality = sanitizeAddressField(municipality, MAX_MUNICIPALITY_LENGTH);

        java.sql.Date sqlDate = new java.sql.Date(dateOfBirth.getTime());

        // 1. Create and insert cluster.
        int clusterId = generateNextClusterId();
        String clusterLabel = (clusterName != null && !clusterName.isEmpty()) ? clusterName : "Default";
        Cluster cluster = new Cluster(clusterId, clusterLabel);
        ClusterDAO.insert(cluster);
        DataManager.getInstance().getCollectionsRegistry().getList("CLUSTER").add(cluster);

        // 2. Create and insert student.
        Student student = new Student(
                studentIdInt,
                firstName, middleName, lastName, nameExt,
                email, status, contact, sqlDate, fare,
                cluster,
                schoolYear, 0, null);

        StudentDAO.insert(student);
        DataManager.getInstance().getCollectionsRegistry().getList("STUDENT").add(student);

        // 3. Create and insert address.
        int addressId = generateNextAddressId();
        Address address = new Address(
                student,
                addressId,
                city,
                municipality,
                street,
                barangay,
                Integer.parseInt(postalCode));
        AddressDAO.insert(address);
        DataManager.getInstance().getCollectionsRegistry().getList("ADDRESS").add(address);

        return student;
    }

    /**
     * Overloaded enrollStudent method when clusterName is not provided.
     */
    public static Student enrollStudent(
            String studentId,
            String firstName, String middleName, String lastName, String nameExt,
            String email, String status, String contact, Date dateOfBirth,
            double fare, String street, String barangay, String city, String municipality,
            String postalCode, Guardian guardian, SchoolYear schoolYear) throws Exception {

        return enrollStudent(studentId, firstName, middleName, lastName, nameExt,
                email, status, contact, dateOfBirth, fare,
                street, barangay, city, municipality, postalCode,
                guardian, null, schoolYear);
    }

    /**
     * Enrolls a student from CSV data by mapping the CSV fields to the
     * enrollment parameters.
     */
    public static Student enrollStudentFromCsv(CsvStudent csvStudent, SchoolYear schoolYear) throws Exception {
        // Generate a new student ID once here.
        int studentId;
        synchronized (STUDENT_ID_LOCK) {
            studentId = generateNextStudentId();
        }
        // Parse address components from CSV.
        String[] addressParts = parseAddress(csvStudent.getAddress(), csvStudent);

        return enrollStudent(
                String.valueOf(studentId), // pass the generated ID
                csvStudent.getFirstName(),
                csvStudent.getMiddleName(),
                csvStudent.getLastName(),
                csvStudent.getNameExtension(),
                csvStudent.getEmail(),
                "Active",
                csvStudent.getContact(),
                new Date(),
                0.0,
                addressParts[0], // street
                addressParts[1], // barangay
                addressParts[2], // city
                addressParts[3], // municipality
                "0", // postalCode (adjust as needed)
                null, // guardian is not included in CSV
                csvStudent.getCluster(),
                schoolYear);
    }

    /**
     * Helper method to enroll multiple students from CSV data.
     */
    public static List<Student> enrollStudentsFromCsv(List<CsvStudent> csvStudents, SchoolYear schoolYear) {
        List<Student> enrolledStudents = new ArrayList<>();
        for (CsvStudent csvStudent : csvStudents) {
            try {
                Student student = enrollStudentFromCsv(csvStudent, schoolYear);
                enrolledStudents.add(student);
            } catch (Exception e) {
                System.err.println("Failed to enroll student with email " + csvStudent.getEmail()
                        + ": " + e.getMessage());
            }
        }
        return enrolledStudents;
    }

    /**
     * Updated parser for CSV address.
     */
    private static String[] parseAddress(String address, CsvStudent csvStudent) {
        if (address == null || address.trim().isEmpty()) {
            return new String[]{"", "", "", ""};
        }
        if (address.contains(",")) {
            String[] parts = address.split(",");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }
            String street, barangay, city, municipality;
            if (parts.length == 2) {
                String part0 = parts[0];
                int idx = part0.toLowerCase().indexOf("brgy");
                if (idx != -1) {
                    String[] tokens = part0.split("\\s+");
                    int foundIndex = -1;
                    for (int i = 0; i < tokens.length; i++) {
                        if (tokens[i].toLowerCase().startsWith("brgy")) {
                            foundIndex = i;
                            break;
                        }
                    }
                    if (foundIndex != -1) {
                        street = foundIndex > 0 ? String.join(" ", java.util.Arrays.copyOfRange(tokens, 0, foundIndex))
                                : "";
                        barangay = String.join(" ", java.util.Arrays.copyOfRange(tokens, foundIndex, tokens.length));
                    } else {
                        street = part0;
                        barangay = "";
                    }
                } else {
                    street = part0;
                    barangay = "";
                }
                String[] cm = parts[1].split("\\s+");
                if (cm.length >= 2) {
                    city = cm[0];
                    municipality = cm[1];
                } else {
                    city = parts[1];
                    municipality = "";
                }
            } else if (parts.length == 3) {
                String first = parts[0].toLowerCase();
                if (first.contains("st") || first.contains("street") || first.contains("ave")) {
                    street = parts[0];
                    String middle = parts[1];
                    municipality = parts[2];
                    int idx = middle.indexOf("Brgy.");
                    if (idx != -1) {
                        barangay = middle.substring(0, idx + "Brgy.".length()).trim();
                        city = middle.substring(idx + "Brgy.".length()).trim();
                    } else {
                        String[] tokens = middle.split("\\s+", 2);
                        if (tokens.length == 2) {
                            barangay = tokens[0].trim();
                            city = tokens[1].trim();
                        } else {
                            barangay = middle;
                            city = "";
                        }
                    }
                } else {
                    street = "";
                    barangay = parts[0];
                    city = parts[1];
                    municipality = parts[2];
                }
            } else if (parts.length >= 4) {
                street = parts[0];
                barangay = parts[1];
                city = parts[2];
                municipality = parts[3];
            } else {
                street = address;
                barangay = "";
                city = "";
                municipality = "";
            }
            return new String[]{
                truncateString(street, MAX_STREET_LENGTH),
                truncateString(barangay, MAX_BARANGAY_LENGTH),
                truncateString(city, MAX_CITY_LENGTH),
                truncateString(municipality, MAX_MUNICIPALITY_LENGTH)
            };
        } else {
            String[] tokens = address.trim().split("\\s+");
            if (tokens.length == 4 && tokens[0].equalsIgnoreCase("Barangay")) {
                return new String[]{
                    "", // street is empty
                    tokens[0] + " " + tokens[1],
                    tokens[2],
                    tokens[3]
                };
            } else if (tokens.length == 2) {
                return new String[]{
                    csvStudent.getMiddleName(),
                    csvStudent.getLastName(),
                    tokens[0],
                    tokens[1]
                };
            } else if (tokens.length == 3) {
                return new String[]{
                    "", // street is empty
                    tokens[0],
                    tokens[1],
                    tokens[2]
                };
            } else {
                return new String[]{address.trim(), "", "", ""};
            }
        }
    }

    private static String truncateString(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    private static String sanitizeAddressField(String field, int maxLength) {
        if (field == null) {
            return "";
        }
        field = field.replaceAll("[\\r\\n]", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return truncateString(field, maxLength);
    }

    // Manual ID generation methods
    private static int generateNextStudentId() {
        synchronized (STUDENT_ID_LOCK) {
            if (lastGeneratedStudentId == -1) {
                lastGeneratedStudentId = DataManager.getInstance().getCollectionsRegistry().getList("STUDENT")
                        .stream()
                        .filter(s -> s instanceof Student)
                        .map(s -> ((Student) s).getStudentID())
                        .mapToInt(id -> id)
                        .max()
                        .orElse(0);
            }
            return ++lastGeneratedStudentId;
        }
    }

    private static int generateNextClusterId() {
        return DataManager.getInstance().getCollectionsRegistry().getList("CLUSTER")
                .stream()
                .filter(c -> c instanceof Cluster)
                .map(c -> ((Cluster) c).getClusterID())
                .mapToInt(id -> id)
                .max()
                .orElse(0) + 1;
    }

    private static int generateNextAddressId() {
        return DataManager.getInstance().getCollectionsRegistry().getList("ADDRESS")
                .stream()
                .filter(a -> a instanceof Address)
                .map(a -> ((Address) a).getAddressID())
                .mapToInt(id -> id)
                .max()
                .orElse(0) + 1;
    }
}
