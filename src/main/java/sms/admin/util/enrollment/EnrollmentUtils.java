package sms.admin.util.enrollment;

import java.util.Date;
import java.util.OptionalInt;

import dev.finalproject.data.AddressDAO;
import dev.finalproject.data.ClusterDAO;
import dev.finalproject.data.StudentDAO;
import dev.finalproject.models.Address;
import dev.finalproject.models.Cluster;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
import sms.admin.App;

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

    /**
     * Enrolls a student by creating a new cluster (using the provided clusterName if available),
     * inserting the student record, and then creating an address record.
     *
     * @param studentId      the student's ID (as a string)
     * @param firstName      the first name
     * @param middleName     the middle name
     * @param lastName       the last name
     * @param nameExt        the name extension
     * @param email          the student's email
     * @param status         the status (e.g., "Active")
     * @param contact        the contact number
     * @param dateOfBirth    the student's date of birth
     * @param fare           the fare amount
     * @param street         the street name (from address)
     * @param barangay       the barangay (from address)
     * @param city           the city (from address)
     * @param municipality   the municipality (from address)
     * @param postalCode     the postal code (as a string)
     * @param guardianName   the guardian's name
     * @param guardianContact the guardian's contact
     * @param clusterName    the cluster name (from CSV; if empty, defaults to "Default")
     * @param schoolYear     the school year
     * @return the created Student object
     * @throws Exception if any error occurs during enrollment
     */
    public static Student enrollStudent(
            String studentId,
            String firstName, String middleName, String lastName, String nameExt,
            String email, String status, String contact, Date dateOfBirth,
            double fare, String street, String barangay, String city, String municipality,
            String postalCode, String guardianName, String guardianContact,
            String clusterName, SchoolYear schoolYear) throws Exception {

        // Truncate all fields to match DB column sizes
        firstName = truncateString(firstName, MAX_NAME_LENGTH);
        middleName = truncateString(middleName, MAX_NAME_LENGTH);
        lastName = truncateString(lastName, MAX_NAME_LENGTH);
        nameExt = truncateString(nameExt, 10);
        email = truncateString(email, MAX_EMAIL_LENGTH);
        status = truncateString(status, MAX_STATUS_LENGTH);
        contact = truncateString(contact, MAX_CONTACT_LENGTH);

        // Format date for MySQL
        java.sql.Date sqlDate = new java.sql.Date(dateOfBirth.getTime());

        // 1. Create and insert cluster using the provided clusterName (or "Default")
        int nextClusterId = generateNextClusterId();
        String clusterLabel = (clusterName != null && !clusterName.isEmpty()) ? clusterName : "Default";
        Cluster cluster = new Cluster(nextClusterId, clusterLabel);
        ClusterDAO.insert(cluster);
        App.COLLECTIONS_REGISTRY.getList("CLUSTER").add(cluster);

        // 2. Create and insert student with foreign key to cluster
        Student student = new Student(
                Integer.parseInt(studentId),
                firstName, middleName, lastName, nameExt,
                email, status, contact, sqlDate, fare,
                cluster,
                schoolYear);
        StudentDAO.insert(student);
        App.COLLECTIONS_REGISTRY.getList("STUDENT").add(student);

        // Wait for student insert to complete before creating address
        Thread.sleep(100); // Small delay to ensure DB consistency

        // 3. Create and insert address with proper field lengths
        Address address = new Address(
                student,
                generateNextAddressId(),
                truncateString(city, MAX_CITY_LENGTH),
                truncateString(municipality, MAX_MUNICIPALITY_LENGTH),
                truncateString(street, MAX_STREET_LENGTH),
                truncateString(barangay, MAX_BARANGAY_LENGTH),
                Integer.parseInt(postalCode));
        AddressDAO.insert(address);
        App.COLLECTIONS_REGISTRY.getList("ADDRESS").add(address);

        return student;
    }

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
     *
     * @param csvStudent the CsvStudent object parsed from CSV data
     * @param schoolYear the school year for enrollment
     * @return the created Student object
     * @throws Exception if any error occurs during enrollment
     */
    public static Student enrollStudentFromCsv(CsvStudent csvStudent, SchoolYear schoolYear) throws Exception {
        String street, barangay, city, municipality;
        
        // Handle mapped and unmapped address formats
        String rawAddress = csvStudent.getAddress();
        if (rawAddress.contains("street ->")) {
            // Handle mapped format
            street = csvStudent.getStreet();
            barangay = csvStudent.getBarangay();
            city = csvStudent.getCity();
            municipality = csvStudent.getMunicipality();
        } else {
            // Handle unmapped format - split by commas
            String[] parts = rawAddress.split(",");
            street = parts.length > 0 ? parts[0].trim() : "";
            barangay = parts.length > 1 ? parts[1].trim() : "";
            city = parts.length > 2 ? parts[2].trim() : "";
            municipality = parts.length > 3 ? parts[3].trim() : "";
        }
        
        return enrollStudent(
            String.valueOf(generateNextId()),
            csvStudent.getFirstName(),
            csvStudent.getMiddleName(),
            csvStudent.getLastName(),
            csvStudent.getNameExtension(), // Use CSV's name extension
            csvStudent.getEmail(),
            "Active", // Default status for CSV imports
            csvStudent.getContact(),
            new Date(), // Current date as default for DOB
            0.0, // Default fare
            truncateString(street, MAX_STREET_LENGTH),
            truncateString(barangay, MAX_BARANGAY_LENGTH),
            truncateString(city, MAX_CITY_LENGTH),
            truncateString(municipality, MAX_MUNICIPALITY_LENGTH),
            "0", // Default postal code
            "",  // Default guardian name
            "",  // Default guardian contact
            csvStudent.getCluster(), // Use cluster from CSV
            schoolYear
        );
    }

    private static String extractPart(String[] parts, int index) {
        if (parts == null || index >= parts.length) return "";
        return truncateString(parts[index].trim(), MAX_STREET_LENGTH);
    }

    // Helper methods to parse CSV address components
    private static String extractStreet(String address) {
        String[] parts = address.split(",");
        return parts.length > 0 ? truncateString(parts[0].trim(), MAX_STREET_LENGTH) : "";
    }

    private static String extractBarangay(String address) {
        String[] parts = address.split(",");
        return parts.length > 1 ? parts[1].trim() : "";
    }

    private static String extractCity(String address) {
        String[] parts = address.split(",");
        return parts.length > 2 ? parts[2].trim() : "";
    }

    private static String extractMunicipality(String address) {
        String[] parts = address.split(",");
        return parts.length > 3 ? parts[3].trim() : "";
    }

    private static Address createAddressFromParts(Student student, String[] parts) {
        return new Address(
                student,
                generateNextAddressId(),
                truncateString(parts.length > 3 ? parts[3].trim() : "", MAX_CITY_LENGTH), // city
                truncateString(parts.length > 2 ? parts[2].trim() : "", MAX_MUNICIPALITY_LENGTH), // municipality  
                truncateString(parts.length > 0 ? parts[0].trim() : "", MAX_STREET_LENGTH), // street
                truncateString(parts.length > 1 ? parts[1].trim() : "", MAX_BARANGAY_LENGTH), // barangay
                0  // zipCode
        );
    }

    private static String[] parseAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return new String[]{"", "", "", ""};
        }
        
        String[] parts = address.split(",");
        String[] result = new String[4];
        
        // Parse address to match DB schema format
        result[0] = truncateString(parts.length > 0 ? parts[0].trim() : "", MAX_STREET_LENGTH);    // street
        result[1] = truncateString(parts.length > 1 ? parts[1].trim() : "", MAX_BARANGAY_LENGTH);  // barangay
        result[2] = truncateString(parts.length > 2 ? parts[2].trim() : "", MAX_MUNICIPALITY_LENGTH); // municipality
        result[3] = truncateString(parts.length > 3 ? parts[3].trim() : "", MAX_CITY_LENGTH);      // city
        
        return result;
    }

    private static String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    private static int generateNextId() {
        OptionalInt maxId = App.COLLECTIONS_REGISTRY.getList("STUDENT").stream()
                .filter(s -> s instanceof Student)
                .map(s -> ((Student) s).getStudentID())
                .mapToInt(id -> id)
                .max();
        return maxId.orElse(0) + 1;
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

    private static boolean clusterExists(int id) {
        return App.COLLECTIONS_REGISTRY.getList("CLUSTER").stream()
                .filter(c -> c instanceof Cluster)
                .map(c -> ((Cluster) c).getClusterID())
                .anyMatch(cid -> cid == id);
    }

    private static boolean studentExists(int id) {
        return App.COLLECTIONS_REGISTRY.getList("STUDENT").stream()
                .filter(s -> s instanceof Student)
                .map(s -> ((Student) s).getStudentID())
                .anyMatch(sid -> sid == id);
    }
}