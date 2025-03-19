package sms.admin.util.enrollment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dev.finalproject.data.AddressDAO;
import dev.finalproject.data.ClusterDAO;
import dev.finalproject.data.StudentDAO;
import dev.finalproject.models.Address;
import dev.finalproject.models.Cluster;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
import sms.admin.App;

public class EnrollmentUtils {
    private static final Object ID_LOCK = new Object();
    private static int lastGeneratedId = -1;

    // Constants matching DB schema column sizes
    private static final int MAX_NAME_LENGTH = 20;
    private static final int MAX_EMAIL_LENGTH = 30;
    private static final int MAX_CONTACT_LENGTH = 15;
    private static final int MAX_STATUS_LENGTH = 20;
    private static final int MAX_STREET_LENGTH = 50;
    private static final int MAX_CITY_LENGTH = 50;
    private static final int MAX_MUNICIPALITY_LENGTH = 50;
    private static final int MAX_BARANGAY_LENGTH = 50;

    /**
     * Enrolls a student by creating a new cluster (using the provided clusterName if available),
     * inserting the student record, and then creating an address record.
     */
    public static Student enrollStudent(
            String studentId,
            String firstName, String middleName, String lastName, String nameExt,
            String email, String status, String contact, Date dateOfBirth,
            double fare, String street, String barangay, String city, String municipality,
            String postalCode, String guardianName, String guardianContact,
            String clusterName, SchoolYear schoolYear) throws Exception {

        // Generate new ID if not provided or invalid.
        int studentIdInt;
        try {
            studentIdInt = Integer.parseInt(studentId);
        } catch (NumberFormatException e) {
            studentIdInt = generateNextId();
        }

        // Validate the ID doesn't already exist.
        if (studentExists(studentIdInt)) {
            throw new Exception("Student ID " + studentIdInt + " already exists");
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
        int nextClusterId = generateNextClusterId();
        String clusterLabel = (clusterName != null && !clusterName.isEmpty()) ? clusterName : "Default";
        Cluster cluster = new Cluster(nextClusterId, clusterLabel);
        ClusterDAO.insert(cluster);
        App.COLLECTIONS_REGISTRY.getList("CLUSTER").add(cluster);

        // 2. Create and insert student.
        Student student = new Student(
                studentIdInt,
                firstName, middleName, lastName, nameExt,
                email, status, contact, sqlDate, fare,
                cluster,
                schoolYear, 0);

        try {
            StudentDAO.insert(student);
            App.COLLECTIONS_REGISTRY.getList("STUDENT").add(student);

            // 3. Create and insert address only after successful student insertion.
            Address address = new Address(
                    student,
                    generateNextAddressId(),
                    city,
                    municipality,
                    street,
                    barangay,
                    Integer.parseInt(postalCode));
            AddressDAO.insert(address);
            App.COLLECTIONS_REGISTRY.getList("ADDRESS").add(address);

            return student;
        } catch (Exception e) {
            // Cleanup on failure.
            try {
                if (studentExists(studentIdInt)) {
                    StudentDAO.delete(student);
                }
            } catch (Exception ex) {
                // Optionally log cleanup failure.
            }
            throw e;
        }
    }

    private static String sanitizeAddressField(String field, int maxLength) {
        if (field == null) return "";
        field = field.replaceAll("[\\r\\n]", " ")
                     .trim()
                     .replaceAll("\\s+", " ");
        return truncateString(field, maxLength);
    }

    /**
     * Overloaded enrollStudent method when clusterName is not provided.
     */
    public static Student enrollStudent(
            String studentId,
            String firstName, String middleName, String lastName, String nameExt,
            String email, String status, String contact, Date dateOfBirth,
            double fare, String street, String barangay, String city, String municipality,
            String postalCode, String guardianName, String guardianContact,
            SchoolYear schoolYear) throws Exception {

        return enrollStudent(studentId, firstName, middleName, lastName, nameExt,
                email, status, contact, dateOfBirth, fare,
                street, barangay, city, municipality, postalCode,
                guardianName, guardianContact, null, schoolYear);
    }

    /**
     * Enrolls a student from CSV data by mapping the CSV fields to the enrollment parameters.
     * Expected address formats:
     *   a) "Street, Barangay, City, Municipality"
     *   b) "Street, Barangay+City, Municipality" (3 parts with heuristic)
     *   c) "Barangay City Municipality" (no commas; 4 tokens with first token "Barangay")
     *   d) "City Municipality" (no commas; 2 tokens) → Use CsvStudent's middle name as street and last name as barangay.
     *   e) "Barangay City Municipality" (no commas; 3 tokens) → Assume no street provided.
     */
    public static Student enrollStudentFromCsv(CsvStudent csvStudent, SchoolYear schoolYear) throws Exception {
        int studentId;
        synchronized (ID_LOCK) {
            studentId = generateNextId();
            if (studentExists(studentId)) {
                throw new Exception("Failed to generate unique student ID");
            }
        }

        // Pass CsvStudent instance so that parseAddress can use its middle and last names if needed.
        String[] addressParts = parseAddress(csvStudent.getAddress(), csvStudent);

        return enrollStudent(
                String.valueOf(studentId),
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
                "0",
                "",
                "",
                csvStudent.getCluster(),
                schoolYear
        );
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
                System.err.println("Failed to enroll student with email " + csvStudent.getEmail() +
                        ": " + e.getMessage());
            }
        }
        return enrolledStudents;
    }

    /**
     * Updated parser for CSV address.
     * Handles various formats:
     *
     * Case 1: If the address contains commas:
     *   - If 4 parts: assumes "Street, Barangay, City, Municipality".
     *   - If 3 parts: uses a heuristic:
     *       * If the first part appears to be a street (contains "st", "street", or "ave"),
     *         splits the middle part (by "Brgy." or whitespace) to get barangay and city.
     *       * Otherwise, assumes no street and maps the parts as: barangay, city, municipality.
     *   - If 2 parts: splits the first part by whitespace looking for a token starting with "brgy".
     *         Then, the second part is assumed to contain "City Municipality" (split by whitespace).
     *
     * Case 2: If no commas are present:
     *   - If exactly 4 tokens and the first token is "Barangay", maps accordingly.
     *   - If exactly 2 tokens, assumes the address provides only city and municipality;
     *     in that case, uses the CsvStudent's middle name as street and last name as barangay.
     *   - If exactly 3 tokens, assumes no street was provided and maps as { "", token[0], token[1], token[2] }.
     *   - Otherwise, treats the entire string as the street.
     *
     * @param address the raw address string.
     * @param csvStudent the CsvStudent instance for additional field values.
     * @return a String array of length 4: {street, barangay, city, municipality}
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
                // New branch: When exactly 2 parts are provided.
                // parts[0] may contain both street and barangay.
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
                        street = foundIndex > 0 ? String.join(" ", java.util.Arrays.copyOfRange(tokens, 0, foundIndex)) : "";
                        barangay = String.join(" ", java.util.Arrays.copyOfRange(tokens, foundIndex, tokens.length));
                    } else {
                        street = part0;
                        barangay = "";
                    }
                } else {
                    street = part0;
                    barangay = "";
                }
                // parts[1] assumed to contain city and municipality.
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
                    // Format: "Street, Barangay+City, Municipality"
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
                    // Assume no street provided.
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
            return new String[] {
                truncateString(street, MAX_STREET_LENGTH),
                truncateString(barangay, MAX_BARANGAY_LENGTH),
                truncateString(city, MAX_CITY_LENGTH),
                truncateString(municipality, MAX_MUNICIPALITY_LENGTH)
            };
        } else {
            // No commas present.
            String[] tokens = address.trim().split("\\s+");
            if (tokens.length == 4 && tokens[0].equalsIgnoreCase("Barangay")) {
                return new String[] {
                    "", // street is empty
                    tokens[0] + " " + tokens[1],
                    tokens[2],
                    tokens[3]
                };
            } else if (tokens.length == 2) {
                return new String[] {
                    csvStudent.getMiddleName(),
                    csvStudent.getLastName(),
                    tokens[0],
                    tokens[1]
                };
            } else if (tokens.length == 3) {
                // Assume no street provided.
                return new String[] {
                    "",        // street is empty
                    tokens[0],
                    tokens[1],
                    tokens[2]
                };
            } else {
                return new String[] { address.trim(), "", "", "" };
            }
        }
    }

    private static String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    private static int generateNextId() {
        synchronized(ID_LOCK) {
            if (lastGeneratedId == -1) {
                lastGeneratedId = App.COLLECTIONS_REGISTRY.getList("STUDENT").stream()
                    .filter(s -> s instanceof Student)
                    .map(s -> ((Student) s).getStudentID())
                    .mapToInt(id -> id)
                    .max()
                    .orElse(0);
            }
            return ++lastGeneratedId;
        }
    }

    private static int generateNextClusterId() {
        return App.COLLECTIONS_REGISTRY.getList("CLUSTER").stream()
                .filter(c -> c instanceof Cluster)
                .map(c -> ((Cluster) c).getClusterID())
                .mapToInt(id -> id)
                .max()
                .orElse(0) + 1;
    }

    private static int generateNextAddressId() {
        return App.COLLECTIONS_REGISTRY.getList("ADDRESS").stream()
                .filter(a -> a instanceof Address)
                .map(a -> ((Address) a).getAddressID())
                .mapToInt(id -> id)
                .max()
                .orElse(0) + 1;
    }

    private static boolean studentExists(int id) {
        return App.COLLECTIONS_REGISTRY.getList("STUDENT").stream()
                .filter(s -> s instanceof Student)
                .map(s -> ((Student) s).getStudentID())
                .anyMatch(sid -> sid == id);
    }
}
